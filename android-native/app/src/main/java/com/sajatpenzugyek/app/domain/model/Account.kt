package com.sajatpenzugyek.app.domain.model

import java.time.Instant

enum class AccountType {
    BANK, SAVINGS, CREDIT_CARD, CASH, INVESTMENT, OTHER
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
)
