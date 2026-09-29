package com.sajatpenzugyek.app

import com.sajatpenzugyek.app.core.utils.CurrencyConverter
import com.sajatpenzugyek.app.core.utils.CurrencyFormatter
import com.sajatpenzugyek.app.domain.model.AppCurrency
import com.sajatpenzugyek.app.domain.model.Category
import com.sajatpenzugyek.app.domain.model.SupportedCurrencies
import com.sajatpenzugyek.app.domain.model.Transaction
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.model.TransactionSource
import com.sajatpenzugyek.app.domain.usecase.CalculateFinancialStatsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class CurrencyConversionTest {

    private val useCase = CalculateFinancialStatsUseCase()

    @Test
    fun testSameCurrencyBypass() {
        // Converting between the same currency must return the exact same minor units with 0 error
        val amount = 123456789L
        assertEquals(amount, CurrencyConverter.convert(amount, "HUF", "HUF", null))
        assertEquals(amount, CurrencyConverter.convert(amount, "EUR", "EUR", null))
        assertEquals(amount, CurrencyConverter.convert(amount, "USD", "USD", BigDecimal.valueOf(99.0)))
    }

    @Test
    fun testMissingRateReturnsNullNeverFallback() {
        // When exchange rate is null, converter must return null (NEVER fallback to 1.0 or pretend 1 EUR = 1 HUF)
        val result = CurrencyConverter.convert(1000000L, "HUF", "EUR", null)
        assertNull("Missing rate must return null, never assume 1:1 fallback", result)
    }

    @Test
    fun testEurToHufConversion() {
        // Rate: 1 EUR = 395.0 HUF
        // 10.00 EUR (1000 minor) * 395 = 3950.00 HUF (395000 minor)
        val rate = BigDecimal("395.0")
        val eurMinor = 1000L // 10.00 EUR
        val hufMinor = CurrencyConverter.convert(eurMinor, "EUR", "HUF", rate)
        assertNotNull(hufMinor)
        assertEquals(395000L, hufMinor)
    }

    @Test
    fun testHufToEurConversion() {
        // Rate: 1 HUF = (1 / 395.0) EUR ~= 0.002531645 EUR
        // 10,000 HUF (1,000,000 minor) -> 25.32 EUR (2532 minor)
        val rate = BigDecimal.ONE.divide(BigDecimal("395.0"), 10, java.math.RoundingMode.HALF_UP)
        val hufMinor = 1000000L // 10,000 HUF
        val eurMinor = CurrencyConverter.convert(hufMinor, "HUF", "EUR", rate)
        assertNotNull(eurMinor)
        assertEquals(2532L, eurMinor)
    }

    @Test
    fun testCalculateCrossRatesFromEurBase() {
        // Base: EUR
        // Rates relative to EUR: HUF = 400.0, USD = 1.08, GBP = 0.85
        val rates = mapOf(
            "EUR" to BigDecimal.ONE,
            "HUF" to BigDecimal("400.0"),
            "USD" to BigDecimal("1.08"),
            "GBP" to BigDecimal("0.85")
        )

        // 1. EUR to HUF
        val eurToHuf = CurrencyConverter.calculateCrossRate("EUR", "HUF", "EUR", rates)
        assertNotNull(eurToHuf)
        assertEquals(400.0, eurToHuf!!.toDouble(), 0.001)

        // 2. HUF to EUR (inverse)
        val hufToEur = CurrencyConverter.calculateCrossRate("HUF", "EUR", "EUR", rates)
        assertNotNull(hufToEur)
        assertEquals(1.0 / 400.0, hufToEur!!.toDouble(), 0.00001)

        // 3. USD to HUF cross rate: (1 / 1.08) * 400.0 ~= 370.37
        val usdToHuf = CurrencyConverter.calculateCrossRate("USD", "HUF", "EUR", rates)
        assertNotNull(usdToHuf)
        assertEquals(400.0 / 1.08, usdToHuf!!.toDouble(), 0.01)

        // 4. Unknown currency returns null
        val jpyRate = CurrencyConverter.calculateCrossRate("JPY", "EUR", "EUR", rates)
        assertNull(jpyRate)
    }

    @Test
    fun testReversibilityPrecision() {
        // Converting 100 EUR -> HUF at 395.0 -> 39,500 HUF -> EUR -> 100 EUR
        val rateEurToHuf = BigDecimal("395.0")
        val rateHufToEur = BigDecimal.ONE.divide(rateEurToHuf, 10, java.math.RoundingMode.HALF_UP)

        val originalEurMinor = 10000L // 100.00 EUR
        val hufMinor = CurrencyConverter.convert(originalEurMinor, "EUR", "HUF", rateEurToHuf)!!
        val backEurMinor = CurrencyConverter.convert(hufMinor, "HUF", "EUR", rateHufToEur)!!

        assertEquals(originalEurMinor, backEurMinor)
    }

    @Test
    fun testNegativeAndZeroAmountPreservation() {
        val rate = BigDecimal("395.0")
        // Zero
        val zero = CurrencyConverter.convert(0L, "EUR", "HUF", rate)
        assertEquals(0L, zero)

        // Negative expense
        val expense = -1000L // -10.00 EUR
        val hufExpense = CurrencyConverter.convert(expense, "EUR", "HUF", rate)
        assertEquals(-395000L, hufExpense)
    }

    @Test
    fun testSupportedCurrenciesDecimals() {
        assertEquals(0, SupportedCurrencies.HUF.decimalPlaces)
        assertEquals(2, SupportedCurrencies.EUR.decimalPlaces)
        assertEquals(2, SupportedCurrencies.USD.decimalPlaces)
        assertEquals(2, SupportedCurrencies.GBP.decimalPlaces)
        assertEquals(2, SupportedCurrencies.CHF.decimalPlaces)
    }

    @Test
    fun testCurrencyFormatting() {
        // HUF
        val hufFormatted = CurrencyFormatter.format(1000000L, "HUF")
        assertTrue(hufFormatted.contains("10") && (hufFormatted.contains("Ft") || hufFormatted.contains("HUF")))

        // EUR
        val eurFormatted = CurrencyFormatter.format(2564L, "EUR")
        assertTrue(eurFormatted.contains("25.64") && eurFormatted.contains("€"))

        // USD
        val usdFormatted = CurrencyFormatter.format(2750L, "USD")
        assertTrue(usdFormatted.contains("27.50") && usdFormatted.contains("$"))

        // formatWithOriginal when currencies differ
        val withOrig = CurrencyFormatter.formatWithOriginal(
            convertedMinor = 2532L,
            displayCurrency = "EUR",
            originalMinor = 1000000L,
            originalCurrency = "HUF"
        )
        assertTrue(withOrig.contains("25.32") && withOrig.contains("10") && withOrig.contains("Ft"))
    }

    @Test
    fun testMultiCurrencyFinancialStatsAggregation() {
        // Transactions in different original currencies
        val txs = listOf(
            // 200 EUR salary (~ 80,000 HUF at 400)
            Transaction(
                id = "tx1",
                accountId = "acc1",
                date = LocalDate.now(),
                amountMinor = 20000L, // 200.00 EUR
                currency = "EUR",
                direction = TransactionDirection.INCOME,
                description = "Euro salary",
                source = TransactionSource.MANUAL,
                fingerprint = "fp1"
            ),
            // 20,000 HUF expense
            Transaction(
                id = "tx2",
                accountId = "acc1",
                date = LocalDate.now(),
                amountMinor = -2000000L, // -20,000 HUF
                currency = "HUF",
                direction = TransactionDirection.EXPENSE,
                description = "Grocery HUF",
                categoryId = "food",
                source = TransactionSource.MANUAL,
                fingerprint = "fp2"
            )
        )

        // Converter that converts everything to HUF (1 EUR = 400 HUF)
        val converterToHuf: (Long, String, LocalDate) -> Long = { amount, curr, _ ->
            when (curr) {
                "HUF" -> amount
                "EUR" -> amount * 400L
                else -> amount
            }
        }

        val statsHuf = useCase.execute(txs, converter = converterToHuf)
        // 200.00 EUR * 400 = 80,000 HUF (8,000,000 minor)
        assertEquals(8000000L, statsHuf.incomeMinor)
        // 20,000 HUF expense (2,000,000 minor)
        assertEquals(2000000L, statsHuf.expenseMinor)
        assertEquals(6000000L, statsHuf.balanceMinor)

        // Converter that converts everything to EUR (400 HUF = 1 EUR)
        val converterToEur: (Long, String, LocalDate) -> Long = { amount, curr, _ ->
            when (curr) {
                "EUR" -> amount
                "HUF" -> amount / 400L
                else -> amount
            }
        }

        val statsEur = useCase.execute(txs, converter = converterToEur)
        // 200.00 EUR income (20000 minor)
        assertEquals(20000L, statsEur.incomeMinor)
        // 20,000 HUF / 400 = 50.00 EUR expense (5000 minor)
        assertEquals(5000L, statsEur.expenseMinor)
        assertEquals(15000L, statsEur.balanceMinor)
    }

    @Test
    fun testManualRateCalculationAndOverride() {
        val customRates = mapOf(
            "EUR" to BigDecimal.ONE,
            "HUF" to BigDecimal("420.0"),
            "USD" to BigDecimal("1.10")
        )

        val rateHuf = CurrencyConverter.calculateCrossRate("EUR", "HUF", "EUR", customRates)
        assertEquals(420.0, rateHuf!!.toDouble(), 0.001)

        val convertedHuf = CurrencyConverter.convert(1000L, "EUR", "HUF", rateHuf)
        assertEquals(420000L, convertedHuf) // 10.00 EUR * 420 = 4200 HUF (420000 minor)
    }
}
