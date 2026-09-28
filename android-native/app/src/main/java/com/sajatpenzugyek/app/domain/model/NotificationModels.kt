package com.sajatpenzugyek.app.domain.model

import java.time.Instant
import java.time.LocalDate

enum class NotificationParseStatus {
    PARSED, IGNORED, NEEDS_REVIEW, FAILED
}

data class RawNotification(
    val packageName: String,
    val applicationLabel: String? = null,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val postedAt: Instant = Instant.now()
)

data class NotificationEvent(
    val id: String,
    val packageName: String,
    val title: String? = null,
    val text: String? = null,
    val postedAt: Instant = Instant.now(),
    val sourceBank: String? = null,
    val processed: Boolean = false,
    val parseStatus: NotificationParseStatus = NotificationParseStatus.NEEDS_REVIEW,
    val transactionId: String? = null,
    val fingerprint: String? = null
)

data class ParsedNotificationTransaction(
    val amountMinor: Long,
    val currency: String = "HUF",
    val direction: TransactionDirection = TransactionDirection.EXPENSE,
    val merchant: String? = null,
    val description: String,
    val date: LocalDate,
    val valueDate: Instant = Instant.now(),
    val suggestedAccountType: AccountType = AccountType.BANK,
    val suggestedAccountName: String? = null,
    val cardLast4: String? = null,
    val confidence: Float = 0.9f,
    val rawTextExcerpt: String? = null
)
