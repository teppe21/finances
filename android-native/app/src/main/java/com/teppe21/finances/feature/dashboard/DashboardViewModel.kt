package com.teppe21.finances.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teppe21.finances.FinancesApp
import com.teppe21.finances.domain.model.CategoryBreakdown
import com.teppe21.finances.domain.model.FinancialStats
import com.teppe21.finances.domain.model.MonthlyTrend
import com.teppe21.finances.domain.model.Transaction
import com.teppe21.finances.domain.usecase.CurrencyAmountConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConvertedTransaction(
    val transaction: Transaction,
    val convertedAmountMinor: Long,
    val displayCurrency: String
)

data class DashboardUiState(
    val stats: FinancialStats = FinancialStats(),
    val recentTransactions: List<Transaction> = emptyList(),
    val recentConvertedTransactions: List<ConvertedTransaction> = emptyList(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val pendingNotificationCount: Int = 0,
    val currency: String = "HUF",
    val isLoading: Boolean = false
)

class DashboardViewModel : ViewModel() {

    private val app = FinancesApp.instance
    private val txRepo = app.transactionRepository
    private val catRepo = app.categoryRepository
    private val notifRepo = app.notificationRepository
    private val statsUseCase = app.calculateStatsUseCase
    private val prefsRepo = app.preferencesRepository
    private val fxRepo = app.exchangeRateRepository

    val uiState: StateFlow<DashboardUiState> = combine(
        txRepo.getAllTransactionsFlow(),
        catRepo.getAllCategoriesFlow(),
        notifRepo.getPendingNotificationsFlow(),
        prefsRepo.preferencesFlow,
        fxRepo.ratesFlow
    ) { transactions, categories, pendingNotifs, prefs, _ ->
        val targetCurrency = prefs.currency
        val converter: CurrencyAmountConverter = { amountMinor, currency, date ->
            fxRepo.convert(amountMinor, currency, targetCurrency, date) ?: amountMinor
        }

        val confirmed = transactions.filter { !it.pending }
        val stats = statsUseCase.execute(confirmed, converter)
        val trends = statsUseCase.calculateMonthlyTrends(confirmed, converter = converter)
        val breakdown = statsUseCase.calculateCategoryBreakdown(confirmed, categories, converter = converter)
        val recent = transactions.take(5)
        val recentConverted = recent.map { tx ->
            ConvertedTransaction(
                transaction = tx,
                convertedAmountMinor = fxRepo.convert(tx.amountMinor, tx.currency, targetCurrency, tx.date) ?: tx.amountMinor,
                displayCurrency = targetCurrency
            )
        }

        DashboardUiState(
            stats = stats,
            recentTransactions = recent,
            recentConvertedTransactions = recentConverted,
            monthlyTrends = trends,
            categoryBreakdown = breakdown,
            pendingNotificationCount = pendingNotifs.size,
            currency = targetCurrency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}
