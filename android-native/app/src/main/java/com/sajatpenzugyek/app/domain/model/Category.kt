package com.sajatpenzugyek.app.domain.model

import android.content.Context
import com.sajatpenzugyek.app.R
import java.time.Instant

data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val isIncome: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun getDisplayName(context: Context): String {
        if (!isDefault) {
            return name
        }
        return when (id) {
            "food" -> context.getString(R.string.cat_food)
            "dining" -> context.getString(R.string.cat_dining)
            "transport" -> context.getString(R.string.cat_transport)
            "subscriptions" -> context.getString(R.string.cat_subscriptions)
            "housing" -> context.getString(R.string.cat_housing)
            "entertainment" -> context.getString(R.string.cat_entertainment)
            "savings" -> context.getString(R.string.cat_savings)
            "income" -> context.getString(R.string.cat_income)
            "health" -> context.getString(R.string.cat_health)
            "shopping" -> context.getString(R.string.cat_shopping)
            "transfers" -> context.getString(R.string.cat_transfers)
            "other" -> context.getString(R.string.cat_other)
            else -> name
        }
    }
}

data class CategoryRule(
    val id: String,
    val categoryId: String,
    val pattern: String,
    val matchType: String = "contains",
    val priority: Int = 10,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now()
)
