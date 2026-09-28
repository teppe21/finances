package com.sajatpenzugyek.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sajatpenzugyek.app.data.local.entity.AccountEntity
import com.sajatpenzugyek.app.data.local.entity.BudgetEntity
import com.sajatpenzugyek.app.data.local.entity.CategoryEntity
import com.sajatpenzugyek.app.data.local.entity.CategoryRuleEntity
import com.sajatpenzugyek.app.data.local.entity.NotificationEventEntity
import com.sajatpenzugyek.app.data.local.entity.ReceiptEntity
import com.sajatpenzugyek.app.data.local.entity.RecurringRuleEntity
import com.sajatpenzugyek.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 5): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getByPeriodFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE externalId = :externalId LIMIT 1")
    suspend fun findByExternalId(externalId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countByCategoryId(categoryId: String): Int

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getAllFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules WHERE isActive = 1 ORDER BY priority DESC")
    fun getAllFlow(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules WHERE isActive = 1 ORDER BY priority DESC")
    suspend fun getAll(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules WHERE categoryId = :categoryId AND isActive = 1 ORDER BY priority DESC")
    fun getRulesForCategoryFlow(categoryId: String): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules WHERE categoryId = :categoryId AND isActive = 1 ORDER BY priority DESC")
    suspend fun getRulesForCategory(categoryId: String): List<CategoryRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<CategoryRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRuleEntity)

    @Update
    suspend fun update(rule: CategoryRuleEntity)

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM category_rules WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_events ORDER BY postedAt DESC")
    fun getAllFlow(): Flow<List<NotificationEventEntity>>

    @Query("SELECT * FROM notification_events WHERE processed = 0 ORDER BY postedAt DESC")
    fun getPendingFlow(): Flow<List<NotificationEventEntity>>

    @Query("SELECT * FROM notification_events WHERE processed = 0 ORDER BY postedAt DESC")
    suspend fun getPending(): List<NotificationEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NotificationEventEntity)

    @Update
    suspend fun update(event: NotificationEventEntity)

    @Delete
    suspend fun delete(event: NotificationEventEntity)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY scannedAt DESC")
    fun getAllFlow(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity)

    @Delete
    suspend fun delete(receipt: ReceiptEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllFlow(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
    fun getAllFlow(): Flow<List<RecurringRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)
}
