package com.teppe21.finances.native.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.teppe21.finances.data.local.database.AppDatabase
import com.teppe21.finances.data.local.entity.NotificationEventEntity
import com.teppe21.finances.data.repository.CategoryRepository
import com.teppe21.finances.data.repository.TransactionRepository
import com.teppe21.finances.domain.model.NotificationParseStatus
import com.teppe21.finances.domain.model.RawNotification
import com.teppe21.finances.domain.usecase.DeduplicationStatus
import com.teppe21.finances.domain.usecase.IngestTransactionUseCase
import com.teppe21.finances.domain.usecase.IngestionInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

enum class AccountMatchTier {
    HIGH,
    MEDIUM,
    LOW
}

class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private val parsers = listOf(
        RevolutParser(),
        GoogleWalletParser(),
        OtpParser(),
        ErsteParser(),
        MbhParser(),
        WiseParser(),
        GenericBankParser()
    )

    companion object {
        private const val TAG = "BankNotifListener"

        private val SECURITY_PATTERNS = listOf(
            "kód", "kod", "code", "passcode", "jelszó", "biztonsági", "verification",
            "hitelesít", "jóváhagyás", "egyszer használatos", "one-time", "sms kód"
        )
        private val TRANSACTION_INDICATORS = listOf(
            "fizetés", "vásárlás", "spent", "paid", "with", "elköltöttél",
            "átutalás", "terhelés", "jóváírás", "received"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName) return // ignore self

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val fullText = "${title ?: ""} ${text ?: ""} ${bigText ?: ""}".lowercase()

        // Pre-filter: Immediately discard OTP / 2FA verification notifications without payment keywords
        val hasSecurityToken = SECURITY_PATTERNS.any { fullText.contains(it) }
        val hasTransactionToken = TRANSACTION_INDICATORS.any { fullText.contains(it) }
        if (hasSecurityToken && !hasTransactionToken) {
            return
        }

        val raw = RawNotification(
            packageName = packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            postedAt = Instant.ofEpochMilli(sbn.postTime)
        )

        serviceScope.launch {
            try {
                processNotification(raw)
            } catch (e: Exception) {
                // Log safe diagnostic information without logging sensitive transaction contents / PII
                Log.e(TAG, "Safe boundary: failed to process notification from package: $packageName", e)
            }
        }
    }

    private suspend fun processNotification(raw: RawNotification) {
        val database = AppDatabase.getInstance(applicationContext)
        val notifDao = database.notificationDao()
        val txDao = database.transactionDao()
        val catDao = database.categoryDao()
        val catRuleDao = database.categoryRuleDao()

        // 1. Find matching parser
        val parser = parsers.firstOrNull { it.canHandle(raw) } ?: return

        // 2. Parse candidate
        val candidate = parser.parse(raw)
        val eventId = UUID.randomUUID().toString()

        if (candidate == null) {
            // Discarded promotion, referral, or non-transaction notification
            // Never store raw notification title or text in SQLite
            notifDao.insert(
                NotificationEventEntity(
                    id = eventId,
                    packageName = raw.packageName,
                    postedAt = raw.postedAt,
                    sourceBank = parser.bankName,
                    processed = true,
                    parseStatus = NotificationParseStatus.IGNORED,
                    transactionId = null,
                    fingerprint = null,
                    reasonCode = "IGNORED_PROMOTION_OR_NON_TRANSACTION"
                )
            )
            return
        }

        // 3. Ingest candidate into unified pipeline
        val txRepo = TransactionRepository(txDao)
        val catRepo = CategoryRepository(catDao, catRuleDao)
        val ingestUseCase = IngestTransactionUseCase(txRepo, catRepo)

        // Find appropriate account using multi-tier confidence matching
        val accounts = database.accountDao().getAll()
        val prefsRepo = com.teppe21.finances.data.local.preferences.UserPreferencesRepository(applicationContext)
        val userMappedAccountId = prefsRepo.getBankMapping(parser.bankName)

        val mappedAccount = accounts.firstOrNull { it.id == userMappedAccountId }
        val exactInstAccount = accounts.firstOrNull { it.institution.equals(parser.bankName, ignoreCase = true) }
        val fuzzyAccount = accounts.firstOrNull {
            it.name.contains(parser.bankName, ignoreCase = true) ||
                it.institution.contains(parser.bankName, ignoreCase = true)
        }

        val (matchedAccount, matchTier) = when {
            mappedAccount != null -> Pair(mappedAccount, AccountMatchTier.HIGH)
            exactInstAccount != null -> Pair(exactInstAccount, AccountMatchTier.HIGH)
            fuzzyAccount != null -> Pair(fuzzyAccount, AccountMatchTier.MEDIUM)
            else -> Pair(null, AccountMatchTier.LOW)
        }

        // If no account could be matched with confidence, hold the notification for review without mutating accounts/balances
        if (matchedAccount == null || matchTier == AccountMatchTier.LOW) {
            notifDao.insert(
                NotificationEventEntity(
                    id = eventId,
                    packageName = raw.packageName,
                    postedAt = raw.postedAt,
                    sourceBank = parser.bankName,
                    processed = false,
                    parseStatus = NotificationParseStatus.NEEDS_REVIEW,
                    transactionId = null,
                    fingerprint = null,
                    reasonCode = "NEEDS_ACCOUNT_MAPPING"
                )
            )
            return
        }

        val effectiveConfidence = when (matchTier) {
            AccountMatchTier.HIGH -> candidate.confidence
            AccountMatchTier.MEDIUM -> minOf(candidate.confidence, 0.80f)
            AccountMatchTier.LOW -> 0.40f
        }
        val isPending = candidate.confidence < 0.90f || matchTier == AccountMatchTier.MEDIUM

        val input = IngestionInput(
            accountId = matchedAccount.id,
            date = candidate.date,
            amountMinor = candidate.amountMinor,
            currency = candidate.currency,
            direction = candidate.direction,
            description = candidate.description,
            merchant = candidate.merchant,
            categoryId = null,
            source = com.teppe21.finances.domain.model.TransactionSource.NOTIFICATION,
            sourceAppPackage = raw.packageName,
            notificationEventId = eventId,
            notes = if (isPending) "Needs Account Review (Bank: ${parser.bankName})" else null,
            confidence = effectiveConfidence,
            pending = isPending
        )

        val output = ingestUseCase.execute(input)

        // 4. Record sanitized metadata event without raw title/text
        val reasonCode = when {
            output.status == DeduplicationStatus.EXACT_DUPLICATE -> "DUPLICATE_CROSS_SOURCE_PAYMENT"
            isPending -> "NEEDS_ACCOUNT_REVIEW"
            output.transaction != null -> "PARSED_SUCCESS"
            else -> "UNRECOGNIZED_STATUS"
        }

        val parseStatus = when {
            output.transaction != null && !isPending -> NotificationParseStatus.PARSED
            output.status == DeduplicationStatus.EXACT_DUPLICATE -> NotificationParseStatus.IGNORED
            else -> NotificationParseStatus.NEEDS_REVIEW
        }

        notifDao.insert(
            NotificationEventEntity(
                id = eventId,
                packageName = raw.packageName,
                postedAt = raw.postedAt,
                sourceBank = parser.bankName,
                processed = output.transaction != null || output.status == DeduplicationStatus.EXACT_DUPLICATE,
                parseStatus = parseStatus,
                transactionId = output.transaction?.id ?: output.duplicateOfId,
                fingerprint = output.transaction?.fingerprint,
                reasonCode = reasonCode
            )
        )
    }
}
