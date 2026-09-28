package com.sajatpenzugyek.app.domain.model

import java.time.LocalDate

data class Budget(
    val id: String,
    val categoryId: String,
    val amountMinor: Long,
    val period: String = "monthly", // monthly / yearly
    val spentMinor: Long = 0L,
    val notes: String? = null
)

data class RecurringRule(
    val id: String,
    val descriptionPattern: String,
    val merchant: String? = null,
    val categoryId: String? = null,
    val frequency: String = "monthly", // weekly, monthly, yearly
    val estimatedAmountMinor: Long = 0L,
    val currency: String = "HUF",
    val lastDate: LocalDate? = null,
    val nextDate: LocalDate? = null,
    val isActive: Boolean = true
)
