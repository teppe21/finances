package com.sajatpenzugyek.app

import com.sajatpenzugyek.app.domain.model.CategoryRule
import com.sajatpenzugyek.app.domain.model.Transaction
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.model.TransactionSource
import com.sajatpenzugyek.app.domain.usecase.CategorizeTransactionUseCase
import com.sajatpenzugyek.app.domain.usecase.DeduplicationStatus
import com.sajatpenzugyek.app.domain.usecase.DeduplicateTransactionUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CategorizationAndDedupTest {

    private val categorizeUseCase = CategorizeTransactionUseCase()
    private val dedupUseCase = DeduplicateTransactionUseCase()

    private val rules = listOf(
        CategoryRule("1", "food", "lidl", "contains", 10),
        CategoryRule("2", "food", "spar", "contains", 10),
        CategoryRule("3", "dining", "wolt", "contains", 10),
        CategoryRule("4", "transport", "mol", "contains", 10),
        CategoryRule("5", "subscriptions", "netflix", "contains", 10),
        CategoryRule("6", "custom_cat", "speciális partner", "exact", 20) // higher priority
    )

    @Test
    fun testCategorizationRules() {
        assertEquals("food", categorizeUseCase.execute("Lidl Budapest", null, rules))
        assertEquals("food", categorizeUseCase.execute("Vásárlás", "SPAR Szupermarket", rules))
        assertEquals("dining", categorizeUseCase.execute("Wolt rendelés", null, rules))
        assertEquals("transport", categorizeUseCase.execute("Tankolás MOL 123", null, rules))
        assertEquals("subscriptions", categorizeUseCase.execute("Netflix.com", null, rules))
        assertEquals("custom_cat", categorizeUseCase.execute("speciális partner", null, rules))
        assertEquals("other", categorizeUseCase.execute("Ismeretlen tétel", null, rules))
    }

    @Test
    fun testDeduplicationExactAndFuzzy() {
        val date = LocalDate.of(2026, 3, 28)
        val fp = dedupUseCase.generateFingerprint(date, -1450000L, "HUF", "LIDL Budapest", "LIDL", null)

        val existingTx = Transaction(
            id = "tx_existing",
            accountId = "acc_1",
            date = date,
            amountMinor = -1450000L,
            currency = "HUF",
            direction = TransactionDirection.EXPENSE,
            description = "LIDL Budapest",
            merchant = "LIDL",
            categoryId = "food",
            source = TransactionSource.CSV,
            fingerprint = fp
        )

        // 1. Exact match test
        val resExact = dedupUseCase.execute(fp, date, -1450000L, "HUF", null, listOf(existingTx))
        assertEquals(DeduplicationStatus.EXACT_DUPLICATE, resExact.status)
        assertEquals("tx_existing", resExact.matchedTransactionId)

        // 2. Fuzzy match (notification received next day with same amount and currency)
        val nextDay = date.plusDays(1)
        val diffFp = dedupUseCase.generateFingerprint(nextDay, -1450000L, "HUF", "Lidl bolt", "Lidl", null)
        val resFuzzy = dedupUseCase.execute(diffFp, nextDay, -1450000L, "HUF", null, listOf(existingTx))
        assertEquals(DeduplicationStatus.POSSIBLE_DUPLICATE, resFuzzy.status)

        // 3. Unique transaction test
        val uniqueFp = dedupUseCase.generateFingerprint(date, -300000L, "HUF", "Kenyér", null, null)
        val resUnique = dedupUseCase.execute(uniqueFp, date, -300000L, "HUF", null, listOf(existingTx))
        assertEquals(DeduplicationStatus.UNIQUE, resUnique.status)
    }
}
