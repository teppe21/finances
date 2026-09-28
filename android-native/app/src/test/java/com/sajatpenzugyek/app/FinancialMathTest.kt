package com.sajatpenzugyek.app

import com.sajatpenzugyek.app.domain.model.Category
import com.sajatpenzugyek.app.domain.model.Transaction
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.model.TransactionSource
import com.sajatpenzugyek.app.domain.usecase.CalculateFinancialStatsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinancialMathTest {

    private val useCase = CalculateFinancialStatsUseCase()

    private fun createTx(
        amountMinor: Long,
        direction: TransactionDirection,
        date: LocalDate = LocalDate.of(2026, 3, 15),
        categoryId: String? = "food"
    ) = Transaction(
        id = "tx_${System.nanoTime()}",
        accountId = "acc_1",
        date = date,
        amountMinor = amountMinor,
        currency = "HUF",
        direction = direction,
        description = "Test",
        categoryId = categoryId,
        source = TransactionSource.MANUAL,
        fingerprint = "fp_${System.nanoTime()}"
    )

    @Test
    fun testIncomeAndExpenseCalculations() {
        val txs = listOf(
            createTx(50000000L, TransactionDirection.INCOME),
            createTx(-1500000L, TransactionDirection.EXPENSE),
            createTx(-3500000L, TransactionDirection.EXPENSE),
            createTx(10000000L, TransactionDirection.TRANSFER) // Should not count as income or expense
        )

        val stats = useCase.execute(txs)
        assertEquals(50000000L, stats.incomeMinor)
        assertEquals(5000000L, stats.expenseMinor)
        assertEquals(45000000L, stats.balanceMinor)
        assertEquals(10000000L, stats.transfersMinor)
        assertEquals(0.9f, stats.savingsRate, 0.01f)
        assertEquals(4, stats.transactionCount)
    }

    @Test
    fun testCategoryBreakdown() {
        val txs = listOf(
            createTx(-1000000L, TransactionDirection.EXPENSE, categoryId = "food"),
            createTx(-2000000L, TransactionDirection.EXPENSE, categoryId = "dining"),
            createTx(-3000000L, TransactionDirection.EXPENSE, categoryId = "food")
        )

        val categories = listOf(
            Category("food", "Élelmiszer"),
            Category("dining", "Étkezés")
        )

        val breakdown = useCase.calculateCategoryBreakdown(txs, categories)
        assertEquals(2, breakdown.size)
        assertEquals("food", breakdown[0].categoryId)
        assertEquals(4000000L, breakdown[0].totalMinor)
        assertEquals(66.6f, breakdown[0].percentage, 1.0f)

        assertEquals("dining", breakdown[1].categoryId)
        assertEquals(2000000L, breakdown[1].totalMinor)
        assertEquals(33.3f, breakdown[1].percentage, 1.0f)
    }

    @Test
    fun testMonthlyTrends() {
        val refDate = LocalDate.of(2026, 3, 28)
        val txs = listOf(
            createTx(20000000L, TransactionDirection.INCOME, date = LocalDate.of(2026, 3, 1)),
            createTx(-5000000L, TransactionDirection.EXPENSE, date = LocalDate.of(2026, 3, 10)),
            createTx(-3000000L, TransactionDirection.EXPENSE, date = LocalDate.of(2026, 2, 15))
        )

        val trends = useCase.calculateMonthlyTrends(txs, monthCount = 3, referenceDate = refDate)
        assertEquals(3, trends.size)

        val mar = trends.first { it.month == "2026-03" }
        assertEquals(20000000L, mar.incomeMinor)
        assertEquals(5000000L, mar.expenseMinor)
        assertEquals(15000000L, mar.balanceMinor)

        val feb = trends.first { it.month == "2026-02" }
        assertEquals(0L, feb.incomeMinor)
        assertEquals(3000000L, feb.expenseMinor)
        assertEquals(-3000000L, feb.balanceMinor)
    }
}
