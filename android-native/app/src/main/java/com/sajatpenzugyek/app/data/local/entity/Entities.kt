package com.sajatpenzugyek.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sajatpenzugyek.app.domain.model.AccountType
import com.sajatpenzugyek.app.domain.model.NotificationParseStatus
import com.sajatpenzugyek.app.domain.model.PaymentMethod
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.model.TransactionSource
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("date"),
        Index("categoryId"),
        Index("fingerprint", unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val date: LocalDate,
    val valueDate: Instant?,
    val amountMinor: Long,
    val currency: String,
    val direction: TransactionDirection,
    val description: String,
    val merchant: String?,
    val categoryId: String?,
    val source: TransactionSource,
    val sourceAppPackage: String?,
    val externalId: String?,
    val fingerprint: String,
    val receiptId: String?,
    val notificationEventId: String?,
    val recurringRuleId: String?,
    val transferId: String?,
    val notes: String?,
    val pending: Boolean,
    val confidence: Float,
    val importedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String,
    val type: AccountType,
    val currency: String,
    val openingBalanceMinor: Long,
    val currentBalanceMinor: Long,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String?,
    val color: String?,
    val isIncome: Boolean,
    val isDefault: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val pattern: String,
    val matchType: String,
    val priority: Int,
    val isActive: Boolean,
    val createdAt: Instant
)

@Entity(tableName = "notification_events")
data class NotificationEventEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Instant,
    val sourceBank: String?,
    val processed: Boolean,
    val parseStatus: NotificationParseStatus,
    val transactionId: String?,
    val fingerprint: String?
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val imageUri: String?,
    val scannedAt: Instant,
    val merchant: String?,
    val date: LocalDate?,
    val totalMinor: Long?,
    val subtotalMinor: Long?,
    val taxMinor: Long?,
    val currency: String,
    val paymentMethod: PaymentMethod,
    val rawOcrText: String?,
    val confidence: Float,
    val transactionId: String?
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val amountMinor: Long,
    val period: String,
    val spentMinor: Long,
    val notes: String?
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val descriptionPattern: String,
    val merchant: String?,
    val categoryId: String?,
    val frequency: String,
    val estimatedAmountMinor: Long,
    val currency: String,
    val lastDate: LocalDate?,
    val nextDate: LocalDate?,
    val isActive: Boolean
)

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["baseCurrency", "targetCurrency", "rateDate"]
)
data class ExchangeRateEntity(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val rateDate: String, // "latest" or "YYYY-MM-DD"
    val timestamp: Instant,
    val provider: String
)

