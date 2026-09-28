package com.sajatpenzugyek.app

import android.app.Application
import com.sajatpenzugyek.app.data.local.database.AppDatabase
import com.sajatpenzugyek.app.data.local.preferences.UserPreferencesRepository
import com.sajatpenzugyek.app.data.repository.AccountRepository
import com.sajatpenzugyek.app.data.repository.CategoryRepository
import com.sajatpenzugyek.app.data.repository.NotificationRepository
import com.sajatpenzugyek.app.data.repository.ReceiptRepository
import com.sajatpenzugyek.app.data.repository.TransactionRepository
import com.sajatpenzugyek.app.domain.usecase.CalculateFinancialStatsUseCase
import com.sajatpenzugyek.app.domain.usecase.CategorizeTransactionUseCase
import com.sajatpenzugyek.app.domain.usecase.DeduplicateTransactionUseCase
import com.sajatpenzugyek.app.domain.usecase.DetectRecurringTransactionsUseCase
import com.sajatpenzugyek.app.domain.usecase.IngestTransactionUseCase

class PenzugyekApp : Application() {

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
        lateinit var instance: PenzugyekApp
            private set
    }
}
