package com.sajatpenzugyek.app.domain.usecase

import com.sajatpenzugyek.app.domain.model.Category
import com.sajatpenzugyek.app.domain.model.CategoryBreakdown
import com.sajatpenzugyek.app.domain.model.FinancialStats
import com.sajatpenzugyek.app.domain.model.MaxExpenseItem
import com.sajatpenzugyek.app.domain.model.MonthlyTrend
import com.sajatpenzugyek.app.domain.model.Transaction
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias CurrencyAmountConverter = (amountMinor: Long, currency: String, date: LocalDate) -> Long

class CalculateFinancialStatsUseCase {

    private fun getConvertedAmount(
        tx: Transaction,
        converter: CurrencyAmountConverter?
    ): Long {
        return if (converter != null) {
            converter(tx.amountMinor, tx.currency, tx.date)
        } else {
            tx.amountMinor
        }
    }

    fun calculateIncome(
        transactions: List<Transaction>,
        converter: CurrencyAmountConverter? = null
    ): Long {
        return transactions.filter {
            it.direction == TransactionDirection.INCOME ||
                (it.direction == TransactionDirection.REFUND && it.amountMinor > 0)
        }.sumOf { abs(getConvertedAmount(it, converter)) }
    }

    fun calculateExpenses(
        transactions: List<Transaction>,
        converter: CurrencyAmountConverter? = null
    ): Long {
        return transactions.filter {
            it.direction == TransactionDirection.EXPENSE
        }.sumOf { abs(getConvertedAmount(it, converter)) }
    }

    fun calculateTransfers(
        transactions: List<Transaction>,
        converter: CurrencyAmountConverter? = null
    ): Long {
        return transactions.filter {
            it.direction == TransactionDirection.TRANSFER
        }.sumOf { abs(getConvertedAmount(it, converter)) }
    }

    fun calculateBalance(
        transactions: List<Transaction>,
        converter: CurrencyAmountConverter? = null
    ): Long {
        return calculateIncome(transactions, converter) - calculateExpenses(transactions, converter)
    }

    fun calculateSavingsRate(incomeMinor: Long, expenseMinor: Long): Float {
        if (incomeMinor <= 0L) return 0.0f
        val savings = incomeMinor - expenseMinor
        val rate = savings.toFloat() / incomeMinor.toFloat()
        return max(-1.0f, min(1.0f, rate))
    }

    fun execute(
        transactions: List<Transaction>,
        converter: CurrencyAmountConverter? = null
    ): FinancialStats {
        val incomeMinor = calculateIncome(transactions, converter)
        val expenseMinor = calculateExpenses(transactions, converter)
        val balanceMinor = incomeMinor - expenseMinor
        val transfersMinor = calculateTransfers(transactions, converter)
        val savingsRate = calculateSavingsRate(incomeMinor, expenseMinor)

        var maxExpense = MaxExpenseItem()
        var expenseCount = 0

        transactions.forEach { t ->
            if (t.direction == TransactionDirection.EXPENSE) {
                val amt = abs(getConvertedAmount(t, converter))
                expenseCount++
                if (amt > maxExpense.amountMinor) {
                    maxExpense = MaxExpenseItem(
                        description = t.merchant ?: t.description,
                        amountMinor = amt
                    )
                }
            }
        }

        val avgExpense = if (expenseCount > 0) expenseMinor / expenseCount else 0L

        return FinancialStats(
            incomeMinor = incomeMinor,
            expenseMinor = expenseMinor,
            balanceMinor = balanceMinor,
            transfersMinor = transfersMinor,
            savingsRate = savingsRate,
            transactionCount = transactions.size,
            maxExpenseItem = maxExpense,
            avgExpenseMinor = avgExpense
        )
    }

    fun calculateMonthlyTrends(
        transactions: List<Transaction>,
        monthCount: Int = 6,
        referenceDate: LocalDate = LocalDate.now(),
        converter: CurrencyAmountConverter? = null
    ): List<MonthlyTrend> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val monthKeys = mutableListOf<String>()

        for (i in (monthCount - 1) downTo 0) {
            val d = referenceDate.minusMonths(i.toLong())
            monthKeys.add(d.format(formatter))
        }

        val incomeMap = monthKeys.associateWith { 0L }.toMutableMap()
        val expenseMap = monthKeys.associateWith { 0L }.toMutableMap()

        transactions.forEach { t ->
            val key = t.date.format(formatter)
            if (incomeMap.containsKey(key)) {
                val convertedAmt = abs(getConvertedAmount(t, converter))
                if (t.direction == TransactionDirection.INCOME) {
                    incomeMap[key] = (incomeMap[key] ?: 0L) + convertedAmt
                } else if (t.direction == TransactionDirection.EXPENSE) {
                    expenseMap[key] = (expenseMap[key] ?: 0L) + convertedAmt
                }
            }
        }

        return monthKeys.map { key ->
            val inc = incomeMap[key] ?: 0L
            val exp = expenseMap[key] ?: 0L
            MonthlyTrend(
                month = key,
                incomeMinor = inc,
                expenseMinor = exp,
                balanceMinor = inc - exp
            )
        }
    }

    fun calculateCategoryBreakdown(
        transactions: List<Transaction>,
        categories: List<Category>,
        converter: CurrencyAmountConverter? = null
    ): List<CategoryBreakdown> {
        val catMap = categories.associateBy { it.id }
        val totals = mutableMapOf<String, Long>()
        val counts = mutableMapOf<String, Int>()
        var totalExpense = 0L

        transactions.filter { it.direction == TransactionDirection.EXPENSE }.forEach { t ->
            val catId = t.categoryId ?: "other"
            val amt = abs(getConvertedAmount(t, converter))
            totalExpense += amt
            totals[catId] = (totals[catId] ?: 0L) + amt
            counts[catId] = (counts[catId] ?: 0) + 1
        }

        return totals.map { (catId, sum) ->
            val cat = catMap[catId]
            CategoryBreakdown(
                categoryId = catId,
                categoryName = cat?.name ?: catId,
                color = cat?.color,
                icon = cat?.icon,
                totalMinor = sum,
                percentage = if (totalExpense > 0L) (sum.toFloat() / totalExpense.toFloat()) * 100f else 0f,
                transactionCount = counts[catId] ?: 0
            )
        }.sortedByDescending { it.totalMinor }
    }
}
