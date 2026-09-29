package com.teppe21.finances.domain.model

import java.time.Instant
import java.time.LocalDate

enum class PaymentMethod {
    CASH, CARD, UNKNOWN
}

data class ReceiptItem(
    val name: String,
    val quantity: Double? = null,
    val unitPriceMinor: Long? = null,
    val totalPriceMinor: Long? = null,
    val categorySuggestion: String? = null
)

data class ReceiptScan(
    val id: String,
    val imageUri: String? = null,
    val scannedAt: Instant = Instant.now(),
    val merchant: String? = null,
    val date: LocalDate? = null,
    val totalMinor: Long? = null,
    val subtotalMinor: Long? = null,
    val taxMinor: Long? = null, // ÁFA
    val currency: String = "HUF",
    val paymentMethod: PaymentMethod = PaymentMethod.UNKNOWN,
    val items: List<ReceiptItem> = emptyList(),
    val rawOcrText: String? = null,
    val confidence: Float = 0.85f,
    val transactionId: String? = null
)
