package com.sajatpenzugyek.app.domain.model

import java.time.Instant

enum class AccountType {
    BANK, SAVINGS, CREDIT_CARD, CASH, INVESTMENT, OTHER;

    fun getDisplayName(): String {
        return when (this) {
            BANK -> "Bank Account"
            SAVINGS -> "Savings"
            CREDIT_CARD -> "Credit Card"
            CASH -> "Cash Wallet"
            INVESTMENT -> "Investment"
            OTHER -> "Other"
        }
    }
}

data class Account(
    val id: String,
    val name: String,
    val institution: String,
    val type: AccountType,
    val currency: String = "HUF",
    val openingBalanceMinor: Long = 0L,
    val currentBalanceMinor: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun getDisplayName(): String {
        return when (id) {
            "acc_cash" -> "Cash Wallet"
            "acc_otp" -> "OTP Current Account"
            "acc_savings" -> "Savings"
            "acc_revolut" -> "Revolut"
            else -> when (name) {
                "Készpénz" -> "Cash Wallet"
                "OTP Folyószámla" -> "OTP Current Account"
                "Megtakarítás" -> "Savings"
                else -> name
            }
        }
    }
}
