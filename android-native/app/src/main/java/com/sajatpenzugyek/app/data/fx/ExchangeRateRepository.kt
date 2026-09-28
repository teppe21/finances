package com.sajatpenzugyek.app.data.fx

import com.sajatpenzugyek.app.core.utils.CurrencyConverter
import com.sajatpenzugyek.app.data.local.dao.ExchangeRateDao
import com.sajatpenzugyek.app.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ExchangeRateRepository(
    private val dao: ExchangeRateDao,
    private val provider: ExchangeRateProvider = CompositeRateProvider(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _ratesState = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())
    val ratesFlow: StateFlow<Map<String, BigDecimal>> = _ratesState.asStateFlow()

    private val _lastSyncInstant = MutableStateFlow<Instant?>(null)
    val lastSyncFlow: StateFlow<Instant?> = _lastSyncInstant.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncingFlow: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOfflineFlow: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        coroutineScope.launch {
            loadCachedRates()
            refreshRates()
        }
    }

    private suspend fun loadCachedRates() = withContext(Dispatchers.IO) {
        val cachedEntities = dao.getAllLatestRates()
        if (cachedEntities.isNotEmpty()) {
            val map = cachedEntities.associate { it.targetCurrency.uppercase() to BigDecimal.valueOf(it.rate) }
                .toMutableMap()
            map["EUR"] = BigDecimal.ONE
            _ratesState.value = map
            _lastSyncInstant.value = cachedEntities.maxOfOrNull { it.timestamp }
        } else {
            // Seed reasonable default baseline rates for fresh offline install
            val defaults = mapOf(
                "EUR" to BigDecimal.ONE,
                "HUF" to BigDecimal.valueOf(395.0),
                "USD" to BigDecimal.valueOf(1.08),
                "GBP" to BigDecimal.valueOf(0.85),
                "CHF" to BigDecimal.valueOf(0.95)
            )
            _ratesState.value = defaults
        }
    }

    suspend fun refreshRates(baseCurrency: String = "EUR"): Result<Unit> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val result = provider.getLatestRates(baseCurrency)
            if (result.isSuccess) {
                val rates = result.getOrThrow()
                val now = Instant.now()

                val entities = rates.map { (code, rate) ->
                    ExchangeRateEntity(
                        baseCurrency = baseCurrency.uppercase(),
                        targetCurrency = code.uppercase(),
                        rate = rate.toDouble(),
                        rateDate = "latest",
                        timestamp = now,
                        provider = provider.name
                    )
                }

                dao.clearLatestRates()
                dao.insertRates(entities)

                _ratesState.value = rates
                _lastSyncInstant.value = now
                _isOffline.value = false
                Result.success(Unit)
            } else {
                _isOffline.value = true
                Result.failure(result.exceptionOrNull() ?: IllegalStateException("Failed to refresh rates"))
            }
        } catch (e: Exception) {
            _isOffline.value = true
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Calculates the exchange rate from fromCurrency to toCurrency.
     * Uses current in-memory cache or cross-rates relative to EUR.
     *
     * @return Multiplier where 1 fromCurrency = rate toCurrency, or null if unavailable.
     */
    fun getRate(
        fromCurrency: String,
        toCurrency: String,
        date: LocalDate? = null
    ): BigDecimal? {
        val from = fromCurrency.uppercase().trim()
        val to = toCurrency.uppercase().trim()

        if (from == to) {
            return BigDecimal.ONE
        }

        val currentRates = _ratesState.value
        return CurrencyConverter.calculateCrossRate(
            fromCurrency = from,
            toCurrency = to,
            baseCurrency = "EUR",
            rates = currentRates
        )
    }

    /**
     * Synchronous conversion helper for minor unit amounts.
     */
    fun convert(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        date: LocalDate? = null
    ): Long? {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return amountMinor
        }
        val rate = getRate(fromCurrency, toCurrency, date)
        return CurrencyConverter.convert(amountMinor, fromCurrency, toCurrency, rate)
    }
}
