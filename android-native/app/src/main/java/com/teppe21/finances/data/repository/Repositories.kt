package com.teppe21.finances.data.repository

import com.teppe21.finances.data.local.dao.AccountDao
import com.teppe21.finances.data.local.dao.CategoryDao
import com.teppe21.finances.data.local.dao.CategoryRuleDao
import com.teppe21.finances.data.local.dao.NotificationDao
import com.teppe21.finances.data.local.dao.ReceiptDao
import com.teppe21.finances.data.local.dao.TransactionDao
import com.teppe21.finances.data.local.entity.AccountEntity
import com.teppe21.finances.data.local.entity.CategoryEntity
import com.teppe21.finances.data.local.entity.CategoryRuleEntity
import com.teppe21.finances.data.local.entity.NotificationEventEntity
import com.teppe21.finances.data.local.entity.ReceiptEntity
import com.teppe21.finances.data.local.entity.TransactionEntity
import com.teppe21.finances.domain.model.Account
import com.teppe21.finances.domain.model.Category
import com.teppe21.finances.domain.model.CategoryRule
import com.teppe21.finances.domain.model.NotificationEvent
import com.teppe21.finances.domain.model.ReceiptScan
import com.teppe21.finances.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactionsFlow(): Flow<List<Transaction>> {
        return transactionDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAllTransactions(): List<Transaction> {
        return transactionDao.getAll().map { it.toDomain() }
    }

    fun getRecentTransactionsFlow(limit: Int = 5): Flow<List<Transaction>> {
        return transactionDao.getRecentFlow(limit).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }

    suspend fun findByFingerprint(fingerprint: String): Transaction? {
        return transactionDao.findByFingerprint(fingerprint)?.toDomain()
    }

    suspend fun findByExternalId(externalId: String): Transaction? {
        return transactionDao.findByExternalId(externalId)?.toDomain()
    }

    suspend fun findPossibleDuplicates(
        amountMinor: Long,
        currency: String,
        minDate: java.time.LocalDate,
        maxDate: java.time.LocalDate
    ): List<Transaction> {
        return transactionDao.findPossibleDuplicates(amountMinor, currency, minDate, maxDate).map { it.toDomain() }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    suspend fun insertAll(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions.map { it.toEntity() })
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }

    suspend fun deleteById(id: String) {
        transactionDao.deleteById(id)
    }
}

class AccountRepository(private val accountDao: AccountDao) {

    fun getAllAccountsFlow(): Flow<List<Account>> {
        return accountDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAllAccounts(): List<Account> {
        return accountDao.getAll().map { it.toDomain() }
    }

    suspend fun getAccountById(id: String): Account? {
        return accountDao.getById(id)?.toDomain()
    }

    suspend fun insertAccount(account: Account) {
        accountDao.insert(account.toEntity())
    }

    suspend fun updateAccount(account: Account) {
        accountDao.update(account.toEntity())
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.delete(account.toEntity())
    }
}

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao
) {

    fun getAllCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAll().map { it.toDomain() }
    }

    fun getAllRulesFlow(): Flow<List<CategoryRule>> {
        return categoryRuleDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAllRules(): List<CategoryRule> {
        return categoryRuleDao.getAll().map { it.toDomain() }
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category.toEntity())
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteById(categoryId)
        categoryRuleDao.deleteByCategoryId(categoryId)
    }

    suspend fun countTransactionsForCategory(categoryId: String, transactionDao: TransactionDao): Int {
        return transactionDao.countByCategoryId(categoryId)
    }

    suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String, transactionDao: TransactionDao) {
        transactionDao.reassignCategory(oldCategoryId, newCategoryId)
    }

    fun getRulesForCategoryFlow(categoryId: String): Flow<List<CategoryRule>> {
        return categoryRuleDao.getRulesForCategoryFlow(categoryId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addKeywordRule(categoryId: String, keyword: String): Boolean {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.isBlank()) return false
        val existing = categoryRuleDao.getRulesForCategory(categoryId)
        if (existing.any { it.pattern.trim().lowercase() == trimmed }) {
            return false
        }
        val rule = CategoryRule(
            id = "rule_${categoryId}_${System.currentTimeMillis()}",
            categoryId = categoryId,
            pattern = trimmed,
            matchType = "contains",
            priority = 50, // User-defined keywords have higher priority than built-in rules (10)
            isActive = true
        )
        categoryRuleDao.insert(rule.toEntity())
        return true
    }

    suspend fun insertRule(rule: CategoryRule) {
        categoryRuleDao.insert(rule.toEntity())
    }

    suspend fun deleteRule(ruleId: String) {
        categoryRuleDao.deleteById(ruleId)
    }
}

class NotificationRepository(private val notificationDao: NotificationDao) {

    fun getPendingNotificationsFlow(): Flow<List<NotificationEvent>> {
        return notificationDao.getPendingFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getPendingNotifications(): List<NotificationEvent> {
        return notificationDao.getPending().map { it.toDomain() }
    }

    suspend fun insertNotification(event: NotificationEvent) {
        notificationDao.insert(event.toEntity())
    }

    suspend fun updateNotification(event: NotificationEvent) {
        notificationDao.update(event.toEntity())
    }
}

class ReceiptRepository(private val receiptDao: ReceiptDao) {

    fun getAllReceiptsFlow(): Flow<List<ReceiptScan>> {
        return receiptDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun insertReceipt(receipt: ReceiptScan) {
        receiptDao.insert(receipt.toEntity())
    }
}

// Extension Mappers between Entities and Domain Models

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    accountId = accountId,
    date = date,
    valueDate = valueDate,
    amountMinor = amountMinor,
    currency = currency,
    direction = direction,
    description = description,
    merchant = merchant,
    categoryId = categoryId,
    source = source,
    sourceAppPackage = sourceAppPackage,
    externalId = externalId,
    fingerprint = fingerprint,
    receiptId = receiptId,
    notificationEventId = notificationEventId,
    recurringRuleId = recurringRuleId,
    transferId = transferId,
    notes = notes,
    pending = pending,
    confidence = confidence,
    importedAt = importedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    accountId = accountId,
    date = date,
    valueDate = valueDate,
    amountMinor = amountMinor,
    currency = currency,
    direction = direction,
    description = description,
    merchant = merchant,
    categoryId = categoryId,
    source = source,
    sourceAppPackage = sourceAppPackage,
    externalId = externalId,
    fingerprint = fingerprint,
    receiptId = receiptId,
    notificationEventId = notificationEventId,
    recurringRuleId = recurringRuleId,
    transferId = transferId,
    notes = notes,
    pending = pending,
    confidence = confidence,
    importedAt = importedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    institution = institution,
    type = type,
    currency = currency,
    openingBalanceMinor = openingBalanceMinor,
    currentBalanceMinor = currentBalanceMinor,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    institution = institution,
    type = type,
    currency = currency,
    openingBalanceMinor = openingBalanceMinor,
    currentBalanceMinor = currentBalanceMinor,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    isIncome = isIncome,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    isIncome = isIncome,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CategoryRuleEntity.toDomain() = CategoryRule(
    id = id,
    categoryId = categoryId,
    pattern = pattern,
    matchType = matchType,
    priority = priority,
    isActive = isActive,
    createdAt = createdAt
)

fun CategoryRule.toEntity() = CategoryRuleEntity(
    id = id,
    categoryId = categoryId,
    pattern = pattern,
    matchType = matchType,
    priority = priority,
    isActive = isActive,
    createdAt = createdAt
)

fun NotificationEventEntity.toDomain() = NotificationEvent(
    id = id,
    packageName = packageName,
    postedAt = postedAt,
    sourceBank = sourceBank,
    processed = processed,
    parseStatus = parseStatus,
    transactionId = transactionId,
    fingerprint = fingerprint,
    reasonCode = reasonCode
)

fun NotificationEvent.toEntity() = NotificationEventEntity(
    id = id,
    packageName = packageName,
    postedAt = postedAt,
    sourceBank = sourceBank,
    processed = processed,
    parseStatus = parseStatus,
    transactionId = transactionId,
    fingerprint = fingerprint,
    reasonCode = reasonCode
)

fun ReceiptEntity.toDomain() = ReceiptScan(
    id = id,
    imageUri = imageUri,
    scannedAt = scannedAt,
    merchant = merchant,
    date = date,
    totalMinor = totalMinor,
    subtotalMinor = subtotalMinor,
    taxMinor = taxMinor,
    currency = currency,
    paymentMethod = paymentMethod,
    rawOcrText = rawOcrText,
    confidence = confidence,
    transactionId = transactionId
)

fun ReceiptScan.toEntity() = ReceiptEntity(
    id = id,
    imageUri = imageUri,
    scannedAt = scannedAt,
    merchant = merchant,
    date = date,
    totalMinor = totalMinor,
    subtotalMinor = subtotalMinor,
    taxMinor = taxMinor,
    currency = currency,
    paymentMethod = paymentMethod,
    rawOcrText = rawOcrText,
    confidence = confidence,
    transactionId = transactionId
)
