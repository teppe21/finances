package com.sajatpenzugyek.app.domain.model

import java.time.Instant
import java.time.LocalDate

enum class TransactionDirection {
    INCOME, EXPENSE, TRANSFER, REFUND, ADJUSTMENT, UNKNOWN
}

enum class TransactionSource {
    MANUAL, CSV, NOTIFICATION, RECEIPT, OPEN_BANKING, BACKUP
}

data class Transaction(
    val id: String,
    val accountId: String,
    val date: LocalDate,
    val valueDate: Instant? = null,
    val amountMinor: Long, // Integer minor units: e.g. 100 HUF = 10000 minor units
    val currency: String = "HUF",
    val direction: TransactionDirection,
    val description: String,
    val merchant: String? = null,
    val categoryId: String? = null,
    val source: TransactionSource,
    val sourceAppPackage: String? = null,
    val externalId: String? = null,
    val fingerprint: String,
    val receiptId: String? = null,
    val notificationEventId: String? = null,
    val recurringRuleId: String? = null,
    val transferId: String? = null,
    val notes: String? = null,
    val pending: Boolean = false,
    val confidence: Float = 1.0f,
    val importedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
