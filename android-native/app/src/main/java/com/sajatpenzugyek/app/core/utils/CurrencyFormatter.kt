package com.sajatpenzugyek.app.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    fun format(amountMinor: Long, currency: String = "HUF"): String {
        val major = abs(amountMinor) / 100.0

        val symbols = DecimalFormatSymbols(Locale("hu", "HU")).apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }

        val pattern = if (currency.equals("HUF", ignoreCase = true)) {
            "#,##0" // HUF is conventionally integer display
        } else {
            "#,##0.00"
        }

        val df = DecimalFormat(pattern, symbols)
        val formatted = df.format(major)

        val suffix = when (currency.uppercase()) {
            "HUF" -> "Ft"
            "EUR" -> "EUR"
            "USD" -> "USD"
            else -> currency
        }

        return "$formatted $suffix"
    }

    fun formatSigned(amountMinor: Long, currency: String = "HUF"): String {
        val base = format(amountMinor, currency)
        return when {
            amountMinor > 0 -> "+$base"
            amountMinor < 0 -> "-$base"
            else -> base
        }
    }
}
