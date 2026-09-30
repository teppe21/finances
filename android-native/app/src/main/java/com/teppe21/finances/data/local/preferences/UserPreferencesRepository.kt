package com.teppe21.finances.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val theme: String = "dark",
    val language: String = "en",
    val currency: String = "HUF",
    val appLockType: String = "off", // "off", "pin", "biometric"
    val pinHash: String = "",
    val pinSalt: String = "",
    val autoImportNotifications: Boolean = true,
    val autoCategorizeReceipts: Boolean = true,
    val autoSyncFxRates: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY = stringPreferencesKey("currency")
        val APP_LOCK_TYPE = stringPreferencesKey("app_lock_type")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val AUTO_IMPORT_NOTIFS = booleanPreferencesKey("auto_import_notifs")
        val AUTO_CAT_RECEIPTS = booleanPreferencesKey("auto_cat_receipts")
        val AUTO_SYNC_FX_RATES = booleanPreferencesKey("auto_sync_fx_rates")
    }

    companion object {
        fun hashPin(pin: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val combined = "$salt:$pin".toByteArray(Charsets.UTF_8)
            val hashBytes = digest.digest(combined)
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            theme = prefs[Keys.THEME] ?: "dark",
            language = prefs[Keys.LANGUAGE] ?: "en",
            currency = prefs[Keys.CURRENCY] ?: "HUF",
            appLockType = prefs[Keys.APP_LOCK_TYPE] ?: "off",
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            autoImportNotifications = prefs[Keys.AUTO_IMPORT_NOTIFS] ?: true,
            autoCategorizeReceipts = prefs[Keys.AUTO_CAT_RECEIPTS] ?: true,
            autoSyncFxRates = prefs[Keys.AUTO_SYNC_FX_RATES] ?: true
        )
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[Keys.CURRENCY] = currency }
    }

    suspend fun setAutoSyncFxRates(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SYNC_FX_RATES] = enabled }
    }

    suspend fun setPinLock(pin: String) {
        val salt = UUID.randomUUID().toString().take(16)
        val hash = hashPin(pin, salt)
        context.dataStore.edit {
            it[Keys.APP_LOCK_TYPE] = "pin"
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
        }
    }

    suspend fun setBiometricLock() {
        context.dataStore.edit {
            it[Keys.APP_LOCK_TYPE] = "biometric"
        }
    }

    suspend fun disableLock() {
        context.dataStore.edit {
            it[Keys.APP_LOCK_TYPE] = "off"
            it[Keys.PIN_HASH] = ""
            it[Keys.PIN_SALT] = ""
        }
    }

    suspend fun verifyPin(inputPin: String): Boolean {
        val prefs = preferencesFlow.first()
        if (prefs.pinHash.isBlank() || prefs.pinSalt.isBlank()) return false
        val computed = hashPin(inputPin, prefs.pinSalt)
        return computed == prefs.pinHash
    }

    suspend fun setAppLock(type: String, pinHash: String = "") {
        context.dataStore.edit {
            it[Keys.APP_LOCK_TYPE] = type
            it[Keys.PIN_HASH] = pinHash
        }
    }

    fun getBankMappingFlow(bankName: String): Flow<String?> {
        val key = stringPreferencesKey("bank_map_${bankName.lowercase().trim()}")
        return context.dataStore.data.map { it[key] }
    }

    suspend fun getBankMapping(bankName: String): String? {
        val key = stringPreferencesKey("bank_map_${bankName.lowercase().trim()}")
        return context.dataStore.data.first()[key]
    }

    suspend fun setBankMapping(bankName: String, accountId: String) {
        val key = stringPreferencesKey("bank_map_${bankName.lowercase().trim()}")
        context.dataStore.edit { it[key] = accountId }
    }
}
