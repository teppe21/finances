package com.sajatpenzugyek.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sajatpenzugyek.app.data.local.dao.AccountDao
import com.sajatpenzugyek.app.data.local.dao.BudgetDao
import com.sajatpenzugyek.app.data.local.dao.CategoryDao
import com.sajatpenzugyek.app.data.local.dao.CategoryRuleDao
import com.sajatpenzugyek.app.data.local.dao.NotificationDao
import com.sajatpenzugyek.app.data.local.dao.ReceiptDao
import com.sajatpenzugyek.app.data.local.dao.RecurringDao
import com.sajatpenzugyek.app.data.local.dao.TransactionDao
import com.sajatpenzugyek.app.data.local.entity.AccountEntity
import com.sajatpenzugyek.app.data.local.entity.BudgetEntity
import com.sajatpenzugyek.app.data.local.entity.CategoryEntity
import com.sajatpenzugyek.app.data.local.entity.CategoryRuleEntity
import com.sajatpenzugyek.app.data.local.entity.NotificationEventEntity
import com.sajatpenzugyek.app.data.local.entity.ReceiptEntity
import com.sajatpenzugyek.app.data.local.entity.RecurringRuleEntity
import com.sajatpenzugyek.app.data.local.entity.TransactionEntity
import com.sajatpenzugyek.app.domain.model.AccountType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
        NotificationEventEntity::class,
        ReceiptEntity::class,
        BudgetEntity::class,
        RecurringRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun notificationDao(): NotificationDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "penzugyek.db"
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    prepopulateDefaults(database)
                }
            }
        }
    }
}

private suspend fun prepopulateDefaults(db: AppDatabase) {
    val now = Instant.now()

    // 1. Prepopulate default accounts (Bank, Revolut, Cash Wallet)
    val accounts = listOf(
        AccountEntity("acc_cash", "Cash Wallet", "Wallet", AccountType.CASH, "HUF", 0L, 0L, true, now, now),
        AccountEntity("acc_otp", "OTP Current Account", "OTP Bank", AccountType.BANK, "HUF", 15000000L, 15000000L, true, now, now),
        AccountEntity("acc_revolut", "Revolut", "Revolut", AccountType.BANK, "HUF", 5000000L, 5000000L, true, now, now),
        AccountEntity("acc_savings", "Savings", "Treasury", AccountType.SAVINGS, "HUF", 50000000L, 50000000L, true, now, now)
    )
    db.accountDao().insertAll(accounts)

    // 2. Prepopulate default categories
    val categories = listOf(
        CategoryEntity("food", "Food & Groceries", "shopping-cart", "#10B981", false, true, now, now),
        CategoryEntity("dining", "Dining & Restaurants", "utensils", "#F59E0B", false, true, now, now),
        CategoryEntity("transport", "Transport & Fuel", "car", "#EF4444", false, true, now, now),
        CategoryEntity("subscriptions", "Subscriptions", "gamepad-2", "#0284C7", false, true, now, now),
        CategoryEntity("housing", "Housing & Utilities", "home", "#3B82F6", false, true, now, now),
        CategoryEntity("entertainment", "Entertainment", "film", "#D97706", false, true, now, now),
        CategoryEntity("savings", "Savings", "piggy-bank", "#06B6D4", false, true, now, now),
        CategoryEntity("income", "Income", "wallet", "#10B981", true, true, now, now),
        CategoryEntity("health", "Health & Pharmacy", "heart-pulse", "#14B8A6", false, true, now, now),
        CategoryEntity("shopping", "Shopping", "shopping-bag", "#F97316", false, true, now, now),
        CategoryEntity("transfers", "Transfers", "arrow-left-right", "#6366F1", false, true, now, now),
        CategoryEntity("other", "Other / Uncategorized", "help-circle", "#6B7280", false, true, now, now)
    )
    db.categoryDao().insertAll(categories)

    // 3. Prepopulate default keyword rules
    val rules = mutableListOf<CategoryRuleEntity>()

    listOf("lidl", "spar", "interspar", "aldi", "tesco", "auchan", "penny", "coop", "cba", "pékség", "lipóti", "príma").forEachIndexed { i, kw ->
        rules.add(CategoryRuleEntity("rule_food_$i", "food", kw, "contains", 10, true, now))
    }
    listOf("wolt", "foodora", "mcdonalds", "kfc", "burger king", "starbucks", "étterem", "kávézó", "pizza", "bistro").forEachIndexed { i, kw ->
        rules.add(CategoryRuleEntity("rule_dining_$i", "dining", kw, "contains", 10, true, now))
    }
    listOf("mol", "shell", "omv", "orlen", "lukoil", "bkk", "máv", "autópálya", "parkolás", "bolt", "uber").forEachIndexed { i, kw ->
        rules.add(CategoryRuleEntity("rule_trans_$i", "transport", kw, "contains", 10, true, now))
    }
    listOf("netflix", "spotify", "google", "apple", "youtube", "patreon", "steam", "disney", "hbo", "chatgpt").forEachIndexed { i, kw ->
        rules.add(CategoryRuleEntity("rule_sub_$i", "subscriptions", kw, "contains", 10, true, now))
    }
    listOf("fizetés", "munkabér", "bér", "salary", "payroll", "jóváírás").forEachIndexed { i, kw ->
        rules.add(CategoryRuleEntity("rule_inc_$i", "income", kw, "contains", 10, true, now))
    }
    listOf("átutalás", "utalás", "átutalt", "átvezetés", "transfer", "pénzküldés", "bank transfer").forEachIndexed { i, kw ->
        rules.add(CategoryRuleEntity("rule_transfer_$i", "transfers", kw, "contains", 10, true, now))
    }

    db.categoryRuleDao().insertAll(rules)
}
