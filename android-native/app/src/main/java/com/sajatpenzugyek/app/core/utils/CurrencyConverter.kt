package com.sajatpenzugyek.app.core.utils

import java.math.BigDecimal
import java.math.RoundingMode

object CurrencyConverter {

    /**
     * Converts an amount in minor units (e.g. cents/fillér) from one currency to another using the provided exchange rate.
     *
     * @param amountMinor The monetary value in integer minor units (100 minor units = 1 major unit).
     * @param fromCurrency ISO 4217 code of source currency (e.g. "HUF", "EUR").
     * @param toCurrency ISO 4217 code of target currency (e.g. "EUR", "HUF").
     * @param rate The multiplier: 1 unit of fromCurrency = rate units of toCurrency.
     * @return Converted amount in minor units, or null if rate is missing/invalid.
     */
    fun convert(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        rate: BigDecimal?
    ): Long? {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return amountMinor
        }

        if (rate == null || rate <= BigDecimal.ZERO) {
            return null // Hard requirement: NEVER use 1.0 as a fallback!
        }

        val original = BigDecimal(amountMinor)
        val converted = original.multiply(rate)
        return converted.setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /**
     * Calculates the exchange rate from fromCurrency to toCurrency given rates relative to a common base (e.g. EUR).
     *
     * @param fromCurrency Source currency code.
     * @param toCurrency Target currency code.
     * @param baseCurrency Common base currency of the rate set (e.g. "EUR").
     * @param rates Map of currency code to rate relative to baseCurrency (e.g. EUR->HUF = 367.1).
     * @return Calculated exchange rate (fromCurrency -> toCurrency), or null if either rate is missing.
     */
    fun calculateCrossRate(
        fromCurrency: String,
        toCurrency: String,
        baseCurrency: String = "EUR",
        rates: Map<String, BigDecimal>
    ): BigDecimal? {
        val from = fromCurrency.uppercase().trim()
        val to = toCurrency.uppercase().trim()
        val base = baseCurrency.uppercase().trim()

        if (from == to) {
            return BigDecimal.ONE
        }

        // Rate of fromCurrency relative to base: 1 base = rateFrom from
        val rateFrom = if (from == base) BigDecimal.ONE else rates[from]
        // Rate of toCurrency relative to base: 1 base = rateTo to
        val rateTo = if (to == base) BigDecimal.ONE else rates[to]

        if (rateFrom == null || rateTo == null || rateFrom <= BigDecimal.ZERO || rateTo <= BigDecimal.ZERO) {
            return null
        }

        // Cross rate: 1 from = (rateTo / rateFrom) to
        return rateTo.divide(rateFrom, 10, RoundingMode.HALF_UP)
    }
}
