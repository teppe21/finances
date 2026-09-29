package com.teppe21.finances.domain.usecase

import com.teppe21.finances.core.utils.TextNormalizer
import com.teppe21.finances.data.repository.CategoryRepository
import com.teppe21.finances.data.repository.TransactionRepository
import com.teppe21.finances.domain.model.Transaction
import com.teppe21.finances.domain.model.TransactionDirection
import com.teppe21.finances.domain.model.TransactionSource
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class IngestionInput(
    val accountId: String,
    val date: LocalDate,
    val amountMinor: Long,
    val currency: String = "HUF",
    val direction: TransactionDirection,
    val description: String,
    val merchant: String? = null,
    val categoryId: String? = null,
    val source: TransactionSource,
    val sourceAppPackage: String? = null,
    val externalId: String? = null,
    val receiptId: String? = null,
    val notificationEventId: String? = null,
    val notes: String? = null,
    val pending: Boolean = false,
    val confidence: Float = 1.0f
)

data class IngestionOutput(
    val status: DeduplicationStatus,
    val transaction: Transaction?,
    val duplicateOfId: String? = null
)

class IngestTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val deduplicateUseCase: DeduplicateTransactionUseCase = DeduplicateTransactionUseCase(),
    private val categorizeUseCase: CategorizeTransactionUseCase = CategorizeTransactionUseCase()
) {

    suspend fun execute(input: IngestionInput): IngestionOutput {
        // 1. Clean merchant and generate deterministic fingerprint
        val cleanMerchant = TextNormalizer.cleanMerchantName(input.merchant ?: input.description)
        val fingerprint = deduplicateUseCase.generateFingerprint(
            date = input.date,
            amountMinor = input.amountMinor,
            currency = input.currency,
            description = input.description,
            merchant = cleanMerchant,
            externalId = input.externalId
        )

        // 2. Check deduplication via indexed database queries (O(1)/O(log N))
        val exactByFp = transactionRepository.findByFingerprint(fingerprint)
        val exactByExtId = if (!input.externalId.isNullOrBlank()) {
            transactionRepository.findByExternalId(input.externalId)
        } else null

        val dedupResult = if (exactByFp != null) {
            DeduplicationResult(DeduplicationStatus.EXACT_DUPLICATE, exactByFp.id, fingerprint)
        } else if (exactByExtId != null) {
            DeduplicationResult(DeduplicationStatus.EXACT_DUPLICATE, exactByExtId.id, fingerprint)
        } else {
            val possibleCandidates = transactionRepository.findPossibleDuplicates(
                amountMinor = input.amountMinor,
                currency = input.currency,
                minDate = input.date.minusDays(2),
                maxDate = input.date.plusDays(2)
            )
            if (possibleCandidates.isNotEmpty()) {
                DeduplicationResult(DeduplicationStatus.POSSIBLE_DUPLICATE, possibleCandidates.first().id, fingerprint)
            } else {
                DeduplicationResult(DeduplicationStatus.UNIQUE, null, fingerprint)
            }
        }

        if (dedupResult.status == DeduplicationStatus.EXACT_DUPLICATE) {
            return IngestionOutput(
                status = DeduplicationStatus.EXACT_DUPLICATE,
                transaction = null,
                duplicateOfId = dedupResult.matchedTransactionId
            )
        }

        // 3. Smart Categorization & Transfer Detection
        val lowerDesc = "${input.description} $cleanMerchant".lowercase()
        val isTransfer = input.direction == TransactionDirection.TRANSFER ||
            lowerDesc.contains("átutalás") || lowerDesc.contains("utalás") ||
            lowerDesc.contains("átutalt") || lowerDesc.contains("átvezetés") ||
            lowerDesc.contains("pénzt küldtél") || lowerDesc.contains("bank transfer") ||
            lowerDesc.contains("transfer to")

        val effectiveDirection = if (isTransfer) TransactionDirection.TRANSFER else input.direction

        val assignedCategory = input.categoryId ?: run {
            if (isTransfer) {
                "transfers"
            } else {
                val rules = categoryRepository.getAllRules()
                categorizeUseCase.execute(input.description, cleanMerchant, rules)
            }
        }

        // 4. Create and save Transaction
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = input.accountId,
            date = input.date,
            valueDate = Instant.now(),
            amountMinor = input.amountMinor,
            currency = input.currency,
            direction = effectiveDirection,
            description = input.description,
            merchant = cleanMerchant.ifBlank { null },
            categoryId = assignedCategory,
            source = input.source,
            sourceAppPackage = input.sourceAppPackage,
            externalId = input.externalId,
            fingerprint = fingerprint,
            receiptId = input.receiptId,
            notificationEventId = input.notificationEventId,
            notes = input.notes,
            pending = input.pending || dedupResult.status == DeduplicationStatus.POSSIBLE_DUPLICATE,
            confidence = input.confidence,
            importedAt = Instant.now(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        transactionRepository.insertTransaction(transaction)

        return IngestionOutput(
            status = dedupResult.status,
            transaction = transaction,
            duplicateOfId = dedupResult.matchedTransactionId
        )
    }
}
