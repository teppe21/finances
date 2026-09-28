package com.sajatpenzugyek.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.PenzugyekApp
import com.sajatpenzugyek.app.domain.model.CategoryBreakdown
import com.sajatpenzugyek.app.domain.model.FinancialStats
import com.sajatpenzugyek.app.domain.model.MonthlyTrend
import com.sajatpenzugyek.app.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val stats: FinancialStats = FinancialStats(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val pendingNotificationCount: Int = 0,
    val currency: String = "HUF",
    val isLoading: Boolean = false
)

class DashboardViewModel : ViewModel() {

    private val app = PenzugyekApp.instance
    private val txRepo = app.transactionRepository
    private val catRepo = app.categoryRepository
    private val notifRepo = app.notificationRepository
    private val statsUseCase = app.calculateStatsUseCase
    private val prefsRepo = app.preferencesRepository

    val uiState: StateFlow<DashboardUiState> = combine(
        txRepo.getAllTransactionsFlow(),
        catRepo.getAllCategoriesFlow(),
        notifRepo.getPendingNotificationsFlow(),
        prefsRepo.preferencesFlow
    ) { transactions, categories, pendingNotifs, prefs ->
        val stats = statsUseCase.execute(transactions)
        val trends = statsUseCase.calculateMonthlyTrends(transactions)
        val breakdown = statsUseCase.calculateCategoryBreakdown(transactions, categories)
        val recent = transactions.take(5)

        DashboardUiState(
            stats = stats,
            recentTransactions = recent,
            monthlyTrends = trends,
            categoryBreakdown = breakdown,
            pendingNotificationCount = pendingNotifs.size,
            currency = prefs.currency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}
