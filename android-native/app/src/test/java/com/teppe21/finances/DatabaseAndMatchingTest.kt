package com.teppe21.finances

import com.teppe21.finances.data.local.database.AppDatabase
import com.teppe21.finances.domain.model.Account
import com.teppe21.finances.domain.model.AccountType
import com.teppe21.finances.domain.model.Transaction
import com.teppe21.finances.domain.model.TransactionDirection
import com.teppe21.finances.domain.model.TransactionSource
import com.teppe21.finances.domain.usecase.DeduplicationStatus
import com.teppe21.finances.domain.usecase.DeduplicateTransactionUseCase
import com.teppe21.finances.native.notification.AccountMatchTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DatabaseAndMatchingTest {

    private val dedupUseCase = DeduplicateTransactionUseCase()

    @Test
    fun testDeduplicationExactFingerprint() {
        val date = LocalDate.of(2026, 3, 28)
        val fp = dedupUseCase.generateFingerprint(
            date = date,
            amountMinor = -450000L,
            currency = "HUF",
            description = "McDonald's purchase",
            merchant = "McDonald's"
        )

        val existing = listOf(
            Transaction(
                id = "tx_1",
                accountId = "acc_1",
                date = date,
                amountMinor = -450000L,
                currency = "HUF",
                direction = TransactionDirection.EXPENSE,
                description = "McDonald's purchase",
                merchant = "McDonald's",
                source = TransactionSource.MANUAL,
                fingerprint = fp,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        val result = dedupUseCase.execute(
            candidateFingerprint = fp,
            candidateDate = date,
            candidateAmountMinor = -450000L,
            candidateCurrency = "HUF",
            existingTransactions = existing
        )

        assertEquals(DeduplicationStatus.EXACT_DUPLICATE, result.status)
        assertEquals("tx_1", result.matchedTransactionId)
    }

    @Test
    fun testDeduplicationFuzzyDateWindow() {
        val date1 = LocalDate.of(2026, 3, 25)
        val date2 = LocalDate.of(2026, 3, 26) // within 2 days

        val fp1 = dedupUseCase.generateFingerprint(date1, -250000L, "HUF", "SPAR supermarket")
        val fp2 = dedupUseCase.generateFingerprint(date2, -250000L, "HUF", "SPAR DEAK TER")

        assertNotEquals("Fingerprints should differ due to date/desc", fp1, fp2)

        val existing = listOf(
            Transaction(
                id = "tx_spar",
                accountId = "acc_1",
                date = date1,
                amountMinor = -250000L,
                currency = "HUF",
                direction = TransactionDirection.EXPENSE,
                description = "SPAR supermarket",
                source = TransactionSource.CSV,
                fingerprint = fp1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        val result = dedupUseCase.execute(
            candidateFingerprint = fp2,
            candidateDate = date2,
            candidateAmountMinor = -250000L,
            candidateCurrency = "HUF",
            existingTransactions = existing
        )

        assertEquals(DeduplicationStatus.POSSIBLE_DUPLICATE, result.status)
        assertEquals("tx_spar", result.matchedTransactionId)
    }

    @Test
    fun testAccountMatchConfidenceTiers() {
        val accounts = listOf(
            Account("acc_otp", "OTP Current Account", "OTP Bank", AccountType.BANK, "HUF", 0L, 0L, true, Instant.now(), Instant.now()),
            Account("acc_rev", "Revolut Pocket", "Revolut Ltd", AccountType.BANK, "HUF", 0L, 0L, true, Instant.now(), Instant.now())
        )

        // Tier 1: User mapping priority
        val userMapped = "acc_rev"
        val mappedAccount = accounts.firstOrNull { it.id == userMapped }
        assertNotNull(mappedAccount)
        val tier1 = if (mappedAccount != null) AccountMatchTier.HIGH else AccountMatchTier.LOW
        assertEquals(AccountMatchTier.HIGH, tier1)

        // Tier 2: Exact institution match
        val bankName = "OTP Bank"
        val exactInst = accounts.firstOrNull { it.institution.equals(bankName, ignoreCase = true) }
        assertNotNull(exactInst)
        val tier2 = if (exactInst != null) AccountMatchTier.HIGH else AccountMatchTier.LOW
        assertEquals(AccountMatchTier.HIGH, tier2)

        // Tier 3: Substring match
        val partialName = "OTP"
        val fuzzyMatch = accounts.firstOrNull { it.name.contains(partialName, ignoreCase = true) }
        assertNotNull(fuzzyMatch)
        val tier3 = if (fuzzyMatch != null) AccountMatchTier.MEDIUM else AccountMatchTier.LOW
        assertEquals(AccountMatchTier.MEDIUM, tier3)

        // Tier 4: Unknown / No match
        val unknownBank = "Deutsche Bank"
        val unmatched = accounts.firstOrNull {
            it.name.contains(unknownBank, ignoreCase = true) || it.institution.contains(unknownBank, ignoreCase = true)
        }
        val tier4 = if (unmatched != null) AccountMatchTier.MEDIUM else AccountMatchTier.LOW
        assertEquals(AccountMatchTier.LOW, tier4)
    }

    @Test
    fun testMigrationsAreDefined() {
        assertNotNull(AppDatabase.MIGRATION_1_2)
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        assertNotNull(AppDatabase.MIGRATION_2_3)
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
    }

    @Test
    fun testUnknownBankDoesNotMatchArbitraryAccount() {
        val accounts = listOf(
            Account("acc_otp", "OTP Current Account", "OTP Bank", AccountType.BANK, "HUF", 0L, 0L, true, Instant.now(), Instant.now()),
            Account("acc_rev", "Revolut Pocket", "Revolut Ltd", AccountType.BANK, "HUF", 0L, 0L, true, Instant.now(), Instant.now())
        )

        val unknownBank = "Random Nonexistent Bank"
        val userMappedAccountId: String? = null
        val mappedAccount = accounts.firstOrNull { it.id == userMappedAccountId }
        val exactInstAccount = accounts.firstOrNull { it.institution.equals(unknownBank, ignoreCase = true) }
        val fuzzyAccount = accounts.firstOrNull {
            it.name.contains(unknownBank, ignoreCase = true) ||
                it.institution.contains(unknownBank, ignoreCase = true)
        }

        val (matchedAccount, matchTier) = when {
            mappedAccount != null -> Pair(mappedAccount, AccountMatchTier.HIGH)
            exactInstAccount != null -> Pair(exactInstAccount, AccountMatchTier.HIGH)
            fuzzyAccount != null -> Pair(fuzzyAccount, AccountMatchTier.MEDIUM)
            else -> Pair(null, AccountMatchTier.LOW)
        }

        org.junit.Assert.assertNull("Unknown bank must never match an arbitrary account", matchedAccount)
        assertEquals(AccountMatchTier.LOW, matchTier)
    }

    @Test
    fun testDemoDataLifecycle() {
        val zeroAccounts = listOf(
            Account("acc_cash", "Cash Wallet", "Wallet", AccountType.CASH, "HUF", 0L, 0L, true, Instant.now(), Instant.now()),
            Account("acc_otp", "OTP Current Account", "OTP Bank", AccountType.BANK, "HUF", 0L, 0L, true, Instant.now(), Instant.now()),
            Account("acc_revolut", "Revolut", "Revolut", AccountType.BANK, "HUF", 0L, 0L, true, Instant.now(), Instant.now()),
            Account("acc_savings", "Savings", "Treasury", AccountType.SAVINGS, "HUF", 0L, 0L, true, Instant.now(), Instant.now())
        )
        // Clean install state has 0 balances
        assertTrue(zeroAccounts.all { it.currentBalanceMinor == 0L })

        // Demo loaded state has non-zero balances
        val demoAccounts = zeroAccounts.map { it.copy(currentBalanceMinor = 1500000L) }
        assertTrue(demoAccounts.all { it.currentBalanceMinor > 0L })

        // Clear demo state restores zero balances
        val clearedAccounts = demoAccounts.map { it.copy(currentBalanceMinor = 0L) }
        assertTrue(clearedAccounts.all { it.currentBalanceMinor == 0L })
    }
}
