package com.sajatpenzugyek.app.domain.usecase

import com.sajatpenzugyek.app.core.utils.TextNormalizer
import com.sajatpenzugyek.app.domain.model.RecurringRule
import com.sajatpenzugyek.app.domain.model.Transaction
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

data class RecurringDetectionResult(
    val recurringTransactionIds: Set<String>,
    val detectedRules: List<RecurringRule>
)

class DetectRecurringTransactionsUseCase {

    fun execute(transactions: List<Transaction>): RecurringDetectionResult {
        val recurringIds = mutableSetOf<String>()
        val detectedRules = mutableListOf<RecurringRule>()

        if (transactions.size < 2) {
            return RecurringDetectionResult(recurringIds, detectedRules)
        }

        // Group by normalized merchant or description
        val groups = transactions.groupBy { t ->
            TextNormalizer.normalizeSearch(t.merchant ?: t.description)
        }.filter { it.key.isNotBlank() && it.value.size >= 2 }

        groups.forEach { (patternKey, items) ->
            val sorted = items.sortedBy { it.date }
            val matchedIds = mutableSetOf<String>()

            for (i in 0 until sorted.size - 1) {
                for (j in i + 1 until sorted.size) {
                    val t1 = sorted[i]
                    val t2 = sorted[j]

                    val months = ChronoUnit.MONTHS.between(t1.date, t2.date)
                    val days = abs(t1.date.dayOfMonth - t2.date.dayOfMonth)
                    val amt1 = abs(t1.amountMinor)
                    val amt2 = abs(t2.amountMinor)
                    val maxAmt = max(amt1, amt2)
                    val diffRatio = if (maxAmt == 0L) 0.0 else abs(amt1 - amt2).toDouble() / maxAmt.toDouble()

                    // Monthly repetition: roughly 1-2 months apart, day of month within 3 days, amount within 10%
                    if (months in 1..2 && days <= 3 && diffRatio <= 0.10) {
                        recurringIds.add(t1.id)
                        recurringIds.add(t2.id)
                        matchedIds.add(t1.id)
                        matchedIds.add(t2.id)
                    }
                }
            }

            if (matchedIds.size >= 2) {
                val matchedList = sorted.filter { matchedIds.contains(it.id) }
                val avgMinor = matchedList.map { abs(it.amountMinor) }.average().toLong()
                val lastTx = matchedList.last()

                detectedRules.add(
                    RecurringRule(
                        id = "recurring_$patternKey",
                        descriptionPattern = lastTx.description,
                        merchant = lastTx.merchant,
                        categoryId = lastTx.categoryId,
                        frequency = "monthly",
                        estimatedAmountMinor = avgMinor,
                        currency = lastTx.currency,
                        lastDate = lastTx.date,
                        nextDate = lastTx.date.plusMonths(1),
                        isActive = true
                    )
                )
            }
        }

        return RecurringDetectionResult(recurringIds, detectedRules)
    }
}
