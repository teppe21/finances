package com.sajatpenzugyek.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.PenzugyekApp
import com.sajatpenzugyek.app.core.utils.TextNormalizer
import com.sajatpenzugyek.app.domain.model.Account
import com.sajatpenzugyek.app.domain.model.Category
import com.sajatpenzugyek.app.domain.model.Transaction
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.model.TransactionSource
import com.sajatpenzugyek.app.domain.usecase.IngestionInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TransactionSortOrder {
    NEWEST,
    OLDEST,
    AMOUNT_DESC,
    AMOUNT_ASC,
    MERCHANT_ASC,
    MERCHANT_DESC
}

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedPeriod: String = "this_month",
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val onlyRecurring: Boolean = false,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.NEWEST,
    val selectedTransaction: Transaction? = null
)

class TransactionsViewModel : ViewModel() {

    private val app = PenzugyekApp.instance
    private val txRepo = app.transactionRepository
    private val catRepo = app.categoryRepository
    private val accountRepo = app.accountRepository
    private val ingestUseCase = app.ingestTransactionUseCase
    private val recurringUseCase = app.detectRecurringUseCase

    private val _selectedPeriod = MutableStateFlow("this_month")
    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _onlyRecurring = MutableStateFlow(false)
    private val _sortOrder = MutableStateFlow(TransactionSortOrder.NEWEST)
    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)

    private data class FilterParams(
        val period: String,
        val category: String,
        val query: String,
        val onlyRecurring: Boolean,
        val sortOrder: TransactionSortOrder
    )

    private val _filterParams = combine(
        _selectedPeriod,
        _selectedCategory,
        _searchQuery,
        _onlyRecurring,
        _sortOrder
    ) { period, category, query, recurring, sort ->
        FilterParams(period, category, query, recurring, sort)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        txRepo.getAllTransactionsFlow(),
        catRepo.getAllCategoriesFlow(),
        accountRepo.getAllAccountsFlow(),
        _filterParams,
        _selectedTransaction
    ) { allTx, cats, accs, filters, selectedTx ->
        val recurringIds = if (filters.onlyRecurring) recurringUseCase.execute(allTx).recurringTransactionIds else emptySet()

        val now = LocalDate.now()
        val filtered = allTx.filter { tx ->
            // 1. Period filter
            val dateMatches = when (filters.period) {
                "this_month" -> tx.date.year == now.year && tx.date.month == now.month
                "last_month" -> {
                    val lastM = now.minusMonths(1)
                    tx.date.year == lastM.year && tx.date.month == lastM.month
                }
                "3_months" -> tx.date.isAfter(now.minusMonths(3))
                "6_months" -> tx.date.isAfter(now.minusMonths(6))
                else -> true
            }

            // 2. Category filter
            val catMatches = filters.category == "All" || tx.categoryId == filters.category

            // 3. Recurring filter
            val recMatches = !filters.onlyRecurring || recurringIds.contains(tx.id)

            // 4. Search query
            val searchMatches = if (filters.query.isBlank()) true else {
                val normQ = TextNormalizer.normalizeSearch(filters.query)
                val target = TextNormalizer.normalizeSearch("${tx.merchant ?: ""} ${tx.description} ${tx.notes ?: ""}")
                target.contains(normQ)
            }

            dateMatches && catMatches && recMatches && searchMatches
        }

        val sorted = when (filters.sortOrder) {
            TransactionSortOrder.NEWEST -> filtered.sortedWith(
                compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt }
            )
            TransactionSortOrder.OLDEST -> filtered.sortedWith(
                compareBy<Transaction> { it.date }.thenBy { it.createdAt }
            )
            TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { kotlin.math.abs(it.amountMinor) }
            TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { kotlin.math.abs(it.amountMinor) }
            TransactionSortOrder.MERCHANT_ASC -> filtered.sortedBy {
                (it.merchant?.ifBlank { null } ?: it.description).lowercase()
            }
            TransactionSortOrder.MERCHANT_DESC -> filtered.sortedByDescending {
                (it.merchant?.ifBlank { null } ?: it.description).lowercase()
            }
        }

        TransactionsUiState(
            transactions = allTx,
            filteredTransactions = sorted,
            categories = cats,
            accounts = accs,
            selectedPeriod = filters.period,
            selectedCategory = filters.category,
            searchQuery = filters.query,
            onlyRecurring = filters.onlyRecurring,
            sortOrder = filters.sortOrder,
            selectedTransaction = selectedTx
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setPeriod(period: String) { _selectedPeriod.value = period }
    fun setSortOrder(order: TransactionSortOrder) { _sortOrder.value = order }
    fun setCategory(category: String) { _selectedCategory.value = category }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun toggleRecurring() { _onlyRecurring.value = !_onlyRecurring.value }
    fun selectTransaction(tx: Transaction?) { _selectedTransaction.value = tx }

    fun addManualTransaction(
        accountId: String,
        amountMinor: Long,
        direction: TransactionDirection,
        description: String,
        merchant: String?,
        categoryId: String?,
        date: LocalDate
    ) {
        viewModelScope.launch {
            ingestUseCase.execute(
                IngestionInput(
                    accountId = accountId,
                    date = date,
                    amountMinor = amountMinor,
                    currency = "HUF",
                    direction = direction,
                    description = description,
                    merchant = merchant,
                    categoryId = categoryId,
                    source = TransactionSource.MANUAL
                )
            )
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            txRepo.updateTransaction(tx)
            _selectedTransaction.value = null
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            txRepo.deleteTransaction(tx)
            _selectedTransaction.value = null
        }
    }
}
