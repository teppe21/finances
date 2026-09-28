package com.sajatpenzugyek.app.data.fx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

interface ExchangeRateProvider {
    val name: String

    /**
     * Fetches current exchange rates relative to [baseCurrency].
     * Returns a map of currencyCode -> rate (e.g. "HUF" -> 395.42).
     */
    suspend fun getLatestRates(baseCurrency: String = "EUR"): Result<Map<String, BigDecimal>>

    /**
     * Fetches historical exchange rate for a specific date.
     */
    suspend fun getHistoricalRate(
        baseCurrency: String,
        targetCurrency: String,
        date: LocalDate
    ): Result<BigDecimal>
}

/**
 * Exchange rate provider backed by Frankfurter API (European Central Bank data).
 * Free, open source, no API key required, highly reliable.
 */
class FrankfurterRateProvider : ExchangeRateProvider {
    override val name: String = "European Central Bank (Frankfurter)"

    override suspend fun getLatestRates(baseCurrency: String): Result<Map<String, BigDecimal>> {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://api.frankfurter.app/latest?from=${baseCurrency.uppercase()}"
                val responseJson = executeGetRequest(urlString)
                val json = JSONObject(responseJson)
                val ratesObj = json.getJSONObject("rates")

                val result = mutableMapOf<String, BigDecimal>()
                result[baseCurrency.uppercase()] = BigDecimal.ONE

                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val code = keys.next()
                    val rateDouble = ratesObj.optDouble(code, -1.0)
                    if (rateDouble > 0.0) {
                        result[code.uppercase()] = BigDecimal.valueOf(rateDouble)
                    }
                }

                if (result.size > 1) {
                    Result.success(result)
                } else {
                    Result.failure(IllegalStateException("No valid rates found in Frankfurter response"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getHistoricalRate(
        baseCurrency: String,
        targetCurrency: String,
        date: LocalDate
    ): Result<BigDecimal> {
        return withContext(Dispatchers.IO) {
            try {
                val dateStr = date.toString() // YYYY-MM-DD
                val urlString = "https://api.frankfurter.app/$dateStr?from=${baseCurrency.uppercase()}&to=${targetCurrency.uppercase()}"
                val responseJson = executeGetRequest(urlString)
                val json = JSONObject(responseJson)
                val ratesObj = json.getJSONObject("rates")
                val rateDouble = ratesObj.optDouble(targetCurrency.uppercase(), -1.0)

                if (rateDouble > 0.0) {
                    Result.success(BigDecimal.valueOf(rateDouble))
                } else {
                    Result.failure(IllegalStateException("Historical rate not found for $targetCurrency on $dateStr"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * Fallback provider backed by open.er-api.com.
 * Free, keyless, global coverage.
 */
class OpenErApiRateProvider : ExchangeRateProvider {
    override val name: String = "Open Exchange Rates API"

    override suspend fun getLatestRates(baseCurrency: String): Result<Map<String, BigDecimal>> {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://open.er-api.com/v6/latest/${baseCurrency.uppercase()}"
                val responseJson = executeGetRequest(urlString)
                val json = JSONObject(responseJson)

                if (json.optString("result") != "success") {
                    return@withContext Result.failure(IllegalStateException("API returned failure status"))
                }

                val ratesObj = json.getJSONObject("rates")
                val result = mutableMapOf<String, BigDecimal>()
                result[baseCurrency.uppercase()] = BigDecimal.ONE

                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val code = keys.next()
                    val rateDouble = ratesObj.optDouble(code, -1.0)
                    if (rateDouble > 0.0) {
                        result[code.uppercase()] = BigDecimal.valueOf(rateDouble)
                    }
                }

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getHistoricalRate(
        baseCurrency: String,
        targetCurrency: String,
        date: LocalDate
    ): Result<BigDecimal> {
        // open.er-api.com free tier does not support arbitrary historical queries without key, fallback to failure
        return Result.failure(UnsupportedOperationException("Historical queries require Frankfurter"))
    }
}

/**
 * Composite provider: tries Frankfurter first; if network/server fails, falls back to OpenErApi.
 */
class CompositeRateProvider(
    private val primary: ExchangeRateProvider = FrankfurterRateProvider(),
    private val secondary: ExchangeRateProvider = OpenErApiRateProvider()
) : ExchangeRateProvider {
    override val name: String = "${primary.name} / ${secondary.name}"

    override suspend fun getLatestRates(baseCurrency: String): Result<Map<String, BigDecimal>> {
        val primaryResult = primary.getLatestRates(baseCurrency)
        if (primaryResult.isSuccess) {
            return primaryResult
        }
        return secondary.getLatestRates(baseCurrency)
    }

    override suspend fun getHistoricalRate(
        baseCurrency: String,
        targetCurrency: String,
        date: LocalDate
    ): Result<BigDecimal> {
        return primary.getHistoricalRate(baseCurrency, targetCurrency, date)
    }
}

private fun executeGetRequest(urlString: String, timeoutMs: Int = 6000): String {
    val url = URL(urlString)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = timeoutMs
    conn.readTimeout = timeoutMs
    conn.setRequestProperty("Accept", "application/json")
    conn.setRequestProperty("User-Agent", "Finances-Android/1.0")

    val responseCode = conn.responseCode
    if (responseCode !in 200..299) {
        throw IllegalStateException("HTTP $responseCode from $urlString")
    }

    val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
    val sb = StringBuilder()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        sb.append(line)
    }
    reader.close()
    conn.disconnect()
    return sb.toString()
}
