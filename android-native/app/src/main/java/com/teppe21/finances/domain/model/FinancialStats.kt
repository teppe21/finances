package com.teppe21.finances.domain.model

data class FinancialStats(
    val incomeMinor: Long = 0L,
    val expenseMinor: Long = 0L,
    val balanceMinor: Long = 0L,
    val transfersMinor: Long = 0L,
    val savingsRate: Float = 0.0f,
    val transactionCount: Int = 0,
    val maxExpenseItem: MaxExpenseItem = MaxExpenseItem(),
    val avgExpenseMinor: Long = 0L
)

data class MaxExpenseItem(
    val description: String = "-",
    val amountMinor: Long = 0L
)

data class MonthlyTrend(
    val month: String, // YYYY-MM
    val incomeMinor: Long,
    val expenseMinor: Long,
    val balanceMinor: Long
)

data class CategoryBreakdown(
    val categoryId: String,
    val categoryName: String,
    val color: String? = null,
    val icon: String? = null,
    val totalMinor: Long = 0L,
    val percentage: Float = 0.0f,
    val transactionCount: Int = 0
)
