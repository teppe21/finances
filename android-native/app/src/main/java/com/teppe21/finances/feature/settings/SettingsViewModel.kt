package com.teppe21.finances.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teppe21.finances.FinancesApp
import com.teppe21.finances.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

class SettingsViewModel : ViewModel() {

    private val app = FinancesApp.instance
    private val prefsRepo = app.preferencesRepository
    private val txRepo = app.transactionRepository
    private val fxRepo = app.exchangeRateRepository

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    val isSyncingRates: StateFlow<Boolean> = fxRepo.isSyncingFlow
    val isRatesOffline: StateFlow<Boolean> = fxRepo.isOfflineFlow
    val lastRatesSync: StateFlow<Instant?> = fxRepo.lastSyncFlow
    val currentRates: StateFlow<Map<String, BigDecimal>> = fxRepo.ratesFlow

    fun setTheme(theme: String) {
        viewModelScope.launch { prefsRepo.setTheme(theme) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { prefsRepo.setLanguage(lang) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { prefsRepo.setCurrency(currency) }
    }

    fun setAutoSyncFxRates(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoSyncFxRates(enabled)
            if (enabled) {
                fxRepo.refreshRates(forceOnline = true)
            }
        }
    }

    fun setManualRate(currency: String, rate: BigDecimal) {
        viewModelScope.launch {
            fxRepo.setManualRate(currency, rate)
        }
    }

    fun setAppLock(type: String, pin: String = "") {
        viewModelScope.launch {
            if (type == "pin" && pin.isNotBlank()) {
                prefsRepo.setPinLock(pin)
            } else if (type == "biometric") {
                prefsRepo.setBiometricLock()
            } else {
                prefsRepo.disableLock()
            }
        }
    }

    fun setPinLock(pin: String) {
        viewModelScope.launch {
            prefsRepo.setPinLock(pin)
        }
    }

    fun setBiometricLock() {
        viewModelScope.launch {
            prefsRepo.setBiometricLock()
        }
    }

    fun disableLock() {
        viewModelScope.launch {
            prefsRepo.disableLock()
        }
    }

    suspend fun verifyPin(inputPin: String): Boolean {
        return prefsRepo.verifyPin(inputPin)
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            fxRepo.refreshRates(forceOnline = true)
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            com.teppe21.finances.data.local.database.loadSampleData(app.database)
        }
    }

    fun clearSampleData() {
        viewModelScope.launch {
            com.teppe21.finances.data.local.database.clearSampleData(app.database)
        }
    }

    fun deleteAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            app.database.clearAllTables()
            prefsRepo.disableLock()
            com.teppe21.finances.data.local.database.prepopulateDefaults(app.database)
            onComplete()
        }
    }
}
