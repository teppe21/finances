package com.sajatpenzugyek.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.PenzugyekApp
import com.sajatpenzugyek.app.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val app = PenzugyekApp.instance
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
    val lastRatesSync: StateFlow<java.time.Instant?> = fxRepo.lastSyncFlow
    val currentRates: StateFlow<Map<String, java.math.BigDecimal>> = fxRepo.ratesFlow

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

    fun setManualRate(currency: String, rate: java.math.BigDecimal) {
        viewModelScope.launch {
            fxRepo.setManualRate(currency, rate)
        }
    }

    fun setAppLock(type: String, pin: String = "") {
        viewModelScope.launch { prefsRepo.setAppLock(type, pin) }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            fxRepo.refreshRates(forceOnline = true)
        }
    }
}
