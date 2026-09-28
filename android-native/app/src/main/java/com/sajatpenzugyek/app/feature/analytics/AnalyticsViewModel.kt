package com.sajatpenzugyek.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.FinancesApp
import com.sajatpenzugyek.app.data.local.preferences.UserPreferences
import com.sajatpenzugyek.app.domain.model.Category
import com.sajatpenzugyek.app.domain.model.CategoryBreakdown
import com.sajatpenzugyek.app.domain.model.FinancialStats
import com.sajatpenzugyek.app.domain.model.MonthlyTrend
import com.sajatpenzugyek.app.domain.model.Transaction
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.usecase.CalculateFinancialStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

enum class AnalyticsPeriod {
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    THIS_YEAR,
    LAST_YEAR,
    CUSTOM
}

data class MerchantSpending(
    val merchant: String,
    val totalMinor: Long,
    val count: Int
)

data class AnalyticsUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val customStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val customEndDate: LocalDate = LocalDate.now(),
    val showCustomDateDialog: Boolean = false,
    val stats: FinancialStats = FinancialStats(),
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val topMerchants: List<MerchantSpending> = emptyList(),
    val selectedCategoryId: String? = null,
    val currency: String = "HUF",
    val dateValidationError: Boolean = false
)

class AnalyticsViewModel : ViewModel() {
    private val app = FinancesApp.instance
    private val txRepo = app.transactionRepository
    private val catRepo = app.categoryRepository
    private val prefsRepo = app.preferencesRepository
    private val fxRepo = app.exchangeRateRepository
    private val statsUseCase = CalculateFinancialStatsUseCase()

    private val _periodState = MutableStateFlow(
        AnalyticsPeriodState(
            period = AnalyticsPeriod.THIS_MONTH,
            customStart = LocalDate.now().withDayOfMonth(1),
            customEnd = LocalDate.now(),
            showDialog = false,
            selectedCatId = null,
            dateError = false
        )
    )

    private data class AnalyticsPeriodState(
        val period: AnalyticsPeriod,
        val customStart: LocalDate,
        val customEnd: LocalDate,
        val showDialog: Boolean,
        val selectedCatId: String?,
        val dateError: Boolean
    )

    private data class AnalyticsSourceData(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val preferences: UserPreferences,
        val rates: Map<String, BigDecimal>
    )

    private val _sourceDataFlow = combine(
        txRepo.getAllTransactionsFlow(),
        catRepo.getAllCategoriesFlow(),
        prefsRepo.preferencesFlow,
        fxRepo.ratesFlow
    ) { transactions, categories, prefs, rates ->
        AnalyticsSourceData(transactions, categories, prefs, rates)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _sourceDataFlow,
        _periodState
    ) { sourceData, periodState ->
        val transactions = sourceData.transactions
        val categories = sourceData.categories
        val prefs = sourceData.preferences
        val (startDate, endDate) = resolveDateRange(
            periodState.period,
            periodState.customStart,
            periodState.customEnd
        )

        val filteredTx = transactions.filter { tx ->
            !tx.date.isBefore(startDate) && !tx.date.isAfter(endDate)
        }

        val targetCurrency = prefs.currency
        val converter: (Long, String, LocalDate) -> Long = { amountMinor, currency, date ->
            fxRepo.convert(amountMinor, currency, targetCurrency, date) ?: amountMinor
        }

        val stats = statsUseCase.execute(filteredTx, converter)
        val breakdown = statsUseCase.calculateCategoryBreakdown(filteredTx, categories, converter = converter)
        val trends = statsUseCase.calculateMonthlyTrends(
            filteredTx,
            monthCount = determineMonthCount(periodState.period, startDate, endDate),
            referenceDate = endDate,
            converter = converter
        )

        // Top merchants calculation with converted currency
        val topMerchants = filteredTx
            .filter { it.direction == TransactionDirection.EXPENSE }
            .groupBy { it.merchant?.trim()?.ifBlank { null } ?: it.description.trim() }
            .map { (name, list) ->
                MerchantSpending(
                    merchant = name,
                    totalMinor = list.sumOf { abs(converter(it.amountMinor, it.currency, it.date)) },
                    count = list.size
                )
            }
            .sortedByDescending { it.totalMinor }
            .take(5)

        AnalyticsUiState(
            period = periodState.period,
            customStartDate = periodState.customStart,
            customEndDate = periodState.customEnd,
            showCustomDateDialog = periodState.showDialog,
            stats = stats,
            categoryBreakdown = breakdown,
            monthlyTrends = trends,
            topMerchants = topMerchants,
            selectedCategoryId = periodState.selectedCatId,
            currency = targetCurrency,
            dateValidationError = periodState.dateError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun selectPeriod(period: AnalyticsPeriod) {
        if (period == AnalyticsPeriod.CUSTOM) {
            _periodState.value = _periodState.value.copy(
                period = period,
                showDialog = true
            )
        } else {
            _periodState.value = _periodState.value.copy(
                period = period,
                showDialog = false,
                dateError = false
            )
        }
    }

    fun openCustomDateDialog() {
        _periodState.value = _periodState.value.copy(showDialog = true)
    }

    fun closeCustomDateDialog() {
        _periodState.value = _periodState.value.copy(showDialog = false)
    }

    fun applyCustomDateRange(start: LocalDate, end: LocalDate) {
        if (start.isAfter(end)) {
            _periodState.value = _periodState.value.copy(dateError = true)
            return
        }
        _periodState.value = _periodState.value.copy(
            period = AnalyticsPeriod.CUSTOM,
            customStart = start,
            customEnd = end,
            showDialog = false,
            dateError = false
        )
    }

    fun toggleCategorySelection(categoryId: String) {
        val current = _periodState.value.selectedCatId
        val next = if (current == categoryId) null else categoryId
        _periodState.value = _periodState.value.copy(selectedCatId = next)
    }

    private fun resolveDateRange(
        period: AnalyticsPeriod,
        customStart: LocalDate,
        customEnd: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (period) {
            AnalyticsPeriod.THIS_MONTH -> {
                val start = now.withDayOfMonth(1)
                val end = YearMonth.from(now).atEndOfMonth()
                start to end
            }
            AnalyticsPeriod.LAST_MONTH -> {
                val lastMonth = now.minusMonths(1)
                val start = lastMonth.withDayOfMonth(1)
                val end = YearMonth.from(lastMonth).atEndOfMonth()
                start to end
            }
            AnalyticsPeriod.LAST_3_MONTHS -> {
                val start = now.minusMonths(3).plusDays(1)
                start to now
            }
            AnalyticsPeriod.LAST_6_MONTHS -> {
                val start = now.minusMonths(6).plusDays(1)
                start to now
            }
            AnalyticsPeriod.THIS_YEAR -> {
                val start = LocalDate.of(now.year, 1, 1)
                val end = LocalDate.of(now.year, 12, 31)
                start to end
            }
            AnalyticsPeriod.LAST_YEAR -> {
                val start = LocalDate.of(now.year - 1, 1, 1)
                val end = LocalDate.of(now.year - 1, 12, 31)
                start to end
            }
            AnalyticsPeriod.CUSTOM -> {
                customStart to customEnd
            }
        }
    }

    private fun determineMonthCount(
        period: AnalyticsPeriod,
        start: LocalDate,
        end: LocalDate
    ): Int {
        return when (period) {
            AnalyticsPeriod.THIS_MONTH, AnalyticsPeriod.LAST_MONTH -> 1
            AnalyticsPeriod.LAST_3_MONTHS -> 3
            AnalyticsPeriod.LAST_6_MONTHS -> 6
            AnalyticsPeriod.THIS_YEAR, AnalyticsPeriod.LAST_YEAR -> 12
            AnalyticsPeriod.CUSTOM -> {
                val months = ((end.year - start.year) * 12 + (end.monthValue - start.monthValue) + 1)
                    .coerceIn(1, 24)
                months
            }
        }
    }
}
