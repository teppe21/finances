package com.sajatpenzugyek.app.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val theme: String = "dark",
    val language: String = "hu",
    val currency: String = "HUF",
    val appLockType: String = "off",
    val pinHash: String = "",
    val autoImportNotifications: Boolean = true,
    val autoCategorizeReceipts: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY = stringPreferencesKey("currency")
        val APP_LOCK_TYPE = stringPreferencesKey("app_lock_type")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val AUTO_IMPORT_NOTIFS = booleanPreferencesKey("auto_import_notifs")
        val AUTO_CAT_RECEIPTS = booleanPreferencesKey("auto_cat_receipts")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            theme = prefs[Keys.THEME] ?: "dark",
            language = prefs[Keys.LANGUAGE] ?: "hu",
            currency = prefs[Keys.CURRENCY] ?: "HUF",
            appLockType = prefs[Keys.APP_LOCK_TYPE] ?: "off",
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            autoImportNotifications = prefs[Keys.AUTO_IMPORT_NOTIFS] ?: true,
            autoCategorizeReceipts = prefs[Keys.AUTO_CAT_RECEIPTS] ?: true
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

    suspend fun setAppLock(type: String, pinHash: String = "") {
        context.dataStore.edit {
            it[Keys.APP_LOCK_TYPE] = type
            it[Keys.PIN_HASH] = pinHash
        }
    }
}
