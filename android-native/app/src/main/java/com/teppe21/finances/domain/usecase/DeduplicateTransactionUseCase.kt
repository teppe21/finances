package com.teppe21.finances.domain.usecase

import com.teppe21.finances.core.utils.TextNormalizer
import com.teppe21.finances.domain.model.Transaction
import java.security.MessageDigest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class DeduplicationStatus {
    UNIQUE,
    EXACT_DUPLICATE,
    POSSIBLE_DUPLICATE
}

data class DeduplicationResult(
    val status: DeduplicationStatus,
    val matchedTransactionId: String? = null,
    val fingerprint: String
)

class DeduplicateTransactionUseCase {

    fun generateFingerprint(
        date: LocalDate,
        amountMinor: Long,
        currency: String,
        description: String,
        merchant: String? = null,
        externalId: String? = null
    ): String {
        val normText = TextNormalizer.normalizeSearch(merchant ?: description)
        val ext = externalId?.trim() ?: ""
        val raw = "$date|$amountMinor|${currency.uppercase()}|$normText|$ext"

        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun execute(
        candidateFingerprint: String,
        candidateDate: LocalDate,
        candidateAmountMinor: Long,
        candidateCurrency: String,
        candidateExternalId: String? = null,
        existingTransactions: List<Transaction>
    ): DeduplicationResult {
        // 1. Check exact fingerprint or externalId match
        for (existing in existingTransactions) {
            if (existing.fingerprint == candidateFingerprint) {
                return DeduplicationResult(
                    status = DeduplicationStatus.EXACT_DUPLICATE,
                    matchedTransactionId = existing.id,
                    fingerprint = candidateFingerprint
                )
            }
            if (!candidateExternalId.isNullOrBlank() && candidateExternalId == existing.externalId) {
                return DeduplicationResult(
                    status = DeduplicationStatus.EXACT_DUPLICATE,
                    matchedTransactionId = existing.id,
                    fingerprint = candidateFingerprint
                )
            }
        }

        // 2. Check fuzzy match (date within 2 days, exact amount, same currency)
        for (existing in existingTransactions) {
            val dayDiff = abs(ChronoUnit.DAYS.between(existing.date, candidateDate))
            if (dayDiff <= 2 &&
                existing.amountMinor == candidateAmountMinor &&
                existing.currency.equals(candidateCurrency, ignoreCase = true)
            ) {
                return DeduplicationResult(
                    status = DeduplicationStatus.POSSIBLE_DUPLICATE,
                    matchedTransactionId = existing.id,
                    fingerprint = candidateFingerprint
                )
            }
        }

        return DeduplicationResult(
            status = DeduplicationStatus.UNIQUE,
            fingerprint = candidateFingerprint
        )
    }
}
