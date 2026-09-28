package com.sajatpenzugyek.app.core.utils

import com.sajatpenzugyek.app.domain.model.SupportedCurrencies
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun format(amountMinor: Long, currency: String = "HUF"): String {
        val curr = SupportedCurrencies.fromCode(currency)
        val major = abs(amountMinor) / 100.0

        val pattern = if (curr.decimalPlaces == 0) {
            "#,##0"
        } else {
            "#,##0." + "0".repeat(curr.decimalPlaces)
        }

        val df = DecimalFormat(pattern, symbols)
        val formattedNumber = df.format(major)

        return when (curr.code) {
            "HUF" -> "$formattedNumber Ft"
            "EUR" -> "€$formattedNumber"
            "USD" -> "$$formattedNumber"
            "GBP" -> "£$formattedNumber"
            "CHF" -> "CHF $formattedNumber"
            else -> "$formattedNumber ${curr.symbol}"
        }
    }

    fun formatSigned(amountMinor: Long, currency: String = "HUF"): String {
        val base = format(amountMinor, currency)
        return when {
            amountMinor > 0 -> "+$base"
            amountMinor < 0 -> "-$base"
            else -> base
        }
    }

    /**
     * Formats an amount with its original value in parentheses if currencies differ.
     * E.g.: "€27.24 (10,000 Ft)"
     */
    fun formatWithOriginal(
        convertedMinor: Long,
        displayCurrency: String,
        originalMinor: Long,
        originalCurrency: String
    ): String {
        val mainStr = formatSigned(convertedMinor, displayCurrency)
        if (displayCurrency.equals(originalCurrency, ignoreCase = true)) {
            return mainStr
        }
        val origStr = formatSigned(originalMinor, originalCurrency)
        return "$mainStr ($origStr)"
    }
}
