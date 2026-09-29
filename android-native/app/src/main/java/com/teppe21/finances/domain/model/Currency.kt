package com.teppe21.finances.domain.model

data class AppCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val decimalPlaces: Int
)

object SupportedCurrencies {
    val HUF = AppCurrency(
        code = "HUF",
        symbol = "Ft",
        name = "Hungarian Forint",
        decimalPlaces = 0
    )

    val EUR = AppCurrency(
        code = "EUR",
        symbol = "€",
        name = "Euro",
        decimalPlaces = 2
    )

    val USD = AppCurrency(
        code = "USD",
        symbol = "$",
        name = "US Dollar",
        decimalPlaces = 2
    )

    val GBP = AppCurrency(
        code = "GBP",
        symbol = "£",
        name = "British Pound",
        decimalPlaces = 2
    )

    val CHF = AppCurrency(
        code = "CHF",
        symbol = "CHF",
        name = "Swiss Franc",
        decimalPlaces = 2
    )

    val ALL = listOf(HUF, EUR, USD, GBP, CHF)

    fun fromCode(code: String?): AppCurrency {
        if (code.isNullOrBlank()) return HUF
        return ALL.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
            ?: AppCurrency(code.uppercase().trim(), code.uppercase().trim(), code.uppercase().trim(), 2)
    }

    fun isValid(code: String?): Boolean {
        if (code.isNullOrBlank()) return false
        return ALL.any { it.code.equals(code.trim(), ignoreCase = true) }
    }
}
