package com.sajatpenzugyek.app.native.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sajatpenzugyek.app.data.local.database.AppDatabase
import com.sajatpenzugyek.app.data.local.entity.NotificationEventEntity
import com.sajatpenzugyek.app.data.repository.CategoryRepository
import com.sajatpenzugyek.app.data.repository.TransactionRepository
import com.sajatpenzugyek.app.domain.model.NotificationParseStatus
import com.sajatpenzugyek.app.domain.model.RawNotification
import com.sajatpenzugyek.app.domain.usecase.IngestTransactionUseCase
import com.sajatpenzugyek.app.domain.usecase.IngestionInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            processNotification(raw)
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

        // Find appropriate account (or default to cash/first bank)
        val accounts = database.accountDao().getAll()
        val matchedAccount = accounts.firstOrNull {
            it.name.contains(parser.bankName, ignoreCase = true) ||
                it.institution.contains(parser.bankName, ignoreCase = true)
        } ?: accounts.firstOrNull()

        val accountId = matchedAccount?.id ?: "acc_otp"

        val input = IngestionInput(
            accountId = accountId,
            date = candidate.date,
            amountMinor = candidate.amountMinor,
            currency = candidate.currency,
            direction = candidate.direction,
            description = candidate.description,
            merchant = candidate.merchant,
            categoryId = null,
            source = com.sajatpenzugyek.app.domain.model.TransactionSource.NOTIFICATION,
            sourceAppPackage = raw.packageName,
            notificationEventId = eventId,
            confidence = candidate.confidence,
            pending = candidate.confidence < 0.90f
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
                processed = output.transaction != null,
                parseStatus = if (output.transaction != null) NotificationParseStatus.PARSED else NotificationParseStatus.NEEDS_REVIEW,
                transactionId = output.transaction?.id,
                fingerprint = output.transaction?.fingerprint
            )
        )
    }
}
