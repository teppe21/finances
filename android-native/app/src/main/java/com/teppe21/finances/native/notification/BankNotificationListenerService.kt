package com.teppe21.finances.native.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.teppe21.finances.data.local.database.AppDatabase
import com.teppe21.finances.data.local.entity.NotificationEventEntity
import com.teppe21.finances.data.repository.CategoryRepository
import com.teppe21.finances.data.repository.TransactionRepository
import com.teppe21.finances.domain.model.NotificationParseStatus
import com.teppe21.finances.domain.model.RawNotification
import com.teppe21.finances.domain.usecase.IngestTransactionUseCase
import com.teppe21.finances.domain.usecase.IngestionInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

import android.util.Log
import kotlinx.coroutines.cancel

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
        OtpParser(),
        ErsteParser(),
        MbhParser(),
        WiseParser(),
        GenericBankParser()
    )

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
            notifDao.insert(
                NotificationEventEntity(
                    id = eventId,
                    packageName = raw.packageName,
                    title = raw.title,
                    text = raw.text,
                    postedAt = raw.postedAt,
                    sourceBank = parser.bankName,
                    processed = true,
                    parseStatus = NotificationParseStatus.IGNORED,
                    transactionId = null,
                    fingerprint = null
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
            else -> Pair(accounts.firstOrNull(), AccountMatchTier.LOW)
        }

        val isLowConfidence = matchTier == AccountMatchTier.LOW
        val accountId = matchedAccount?.id ?: ""
        val effectiveConfidence = when (matchTier) {
            AccountMatchTier.HIGH -> candidate.confidence
            AccountMatchTier.MEDIUM -> minOf(candidate.confidence, 0.80f)
            AccountMatchTier.LOW -> 0.40f
        }
        val isPending = candidate.confidence < 0.90f || isLowConfidence

        val input = IngestionInput(
            accountId = accountId,
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
            notes = if (isLowConfidence) "Needs Account Review (Bank: ${parser.bankName})" else null,
            confidence = effectiveConfidence,
            pending = isPending
        )

        val output = ingestUseCase.execute(input)

        // 4. Record event
        notifDao.insert(
            NotificationEventEntity(
                id = eventId,
                packageName = raw.packageName,
                title = raw.title,
                text = raw.text,
                postedAt = raw.postedAt,
                sourceBank = parser.bankName,
                processed = output.transaction != null && !isLowConfidence,
                parseStatus = if (output.transaction != null && !isLowConfidence) NotificationParseStatus.PARSED else NotificationParseStatus.NEEDS_REVIEW,
                transactionId = output.transaction?.id,
                fingerprint = output.transaction?.fingerprint
            )
        )
    }

    companion object {
        private const val TAG = "BankNotifListener"
    }
}

