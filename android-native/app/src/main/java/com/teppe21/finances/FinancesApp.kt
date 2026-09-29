package com.teppe21.finances

import android.app.Application
import com.teppe21.finances.data.local.database.AppDatabase
import com.teppe21.finances.data.local.preferences.UserPreferencesRepository
import com.teppe21.finances.data.repository.AccountRepository
import com.teppe21.finances.data.repository.CategoryRepository
import com.teppe21.finances.data.repository.NotificationRepository
import com.teppe21.finances.data.repository.ReceiptRepository
import com.teppe21.finances.data.repository.TransactionRepository
import com.teppe21.finances.domain.usecase.CalculateFinancialStatsUseCase
import com.teppe21.finances.domain.usecase.CategorizeTransactionUseCase
import com.teppe21.finances.domain.usecase.DeduplicateTransactionUseCase
import com.teppe21.finances.domain.usecase.DetectRecurringTransactionsUseCase
import com.teppe21.finances.domain.usecase.IngestTransactionUseCase

class FinancesApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var transactionRepository: TransactionRepository
        private set
    lateinit var accountRepository: AccountRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var notificationRepository: NotificationRepository
        private set
    lateinit var receiptRepository: ReceiptRepository
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var exchangeRateRepository: com.teppe21.finances.data.fx.ExchangeRateRepository
        private set

    // Use cases
    lateinit var calculateStatsUseCase: CalculateFinancialStatsUseCase
        private set
    lateinit var ingestTransactionUseCase: IngestTransactionUseCase
        private set
    lateinit var detectRecurringUseCase: DetectRecurringTransactionsUseCase
        private set
    lateinit var categorizeUseCase: CategorizeTransactionUseCase
        private set
    lateinit var deduplicateUseCase: DeduplicateTransactionUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        java.util.Locale.setDefault(java.util.Locale.ENGLISH)
        val config = resources.configuration
        config.setLocale(java.util.Locale.ENGLISH)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        database = AppDatabase.getInstance(this)

        transactionRepository = TransactionRepository(database.transactionDao())
        accountRepository = AccountRepository(database.accountDao())
        categoryRepository = CategoryRepository(database.categoryDao(), database.categoryRuleDao())
        notificationRepository = NotificationRepository(database.notificationDao())
        receiptRepository = ReceiptRepository(database.receiptDao())
        preferencesRepository = UserPreferencesRepository(this)
        exchangeRateRepository = com.teppe21.finances.data.fx.ExchangeRateRepository(
            dao = database.exchangeRateDao(),
            prefsRepo = preferencesRepository
        )

        calculateStatsUseCase = CalculateFinancialStatsUseCase()
        categorizeUseCase = CategorizeTransactionUseCase()
        deduplicateUseCase = DeduplicateTransactionUseCase()
        detectRecurringUseCase = DetectRecurringTransactionsUseCase()

        ingestTransactionUseCase = IngestTransactionUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            deduplicateUseCase = deduplicateUseCase,
            categorizeUseCase = categorizeUseCase
        )
    }

    companion object {
        lateinit var instance: FinancesApp
            private set
    }
}
