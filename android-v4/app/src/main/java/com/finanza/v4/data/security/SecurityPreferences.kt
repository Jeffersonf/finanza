package com.finanza.v4.data.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore("security")

class SecurityPreferences(context: Context) {
    private val dataStore = context.applicationContext.securityDataStore

    val config: Flow<AppLockConfig> = dataStore.data.map { prefs ->
        AppLockConfig(
            enabled = prefs[LOCK_ENABLED] ?: false,
            biometricsEnabled = prefs[BIOMETRICS_ENABLED] ?: false,
            pinHash = prefs[PIN_HASH].orEmpty()
        )
    }

    suspend fun get(): AppLockConfig = config.first()

    suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[LOCK_ENABLED] = enabled }
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BIOMETRICS_ENABLED] = enabled }
    }

    suspend fun savePin(pin: String) {
        dataStore.edit { prefs ->
            prefs[PIN_HASH] = hashPin(pin)
            prefs[LOCK_ENABLED] = true
        }
    }

    suspend fun clearPin() {
        dataStore.edit { prefs -> prefs.remove(PIN_HASH) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val current = get()
        if (current.pinHash.isBlank()) return false
        return current.pinHash == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }
}

data class AppLockConfig(
    val enabled: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val pinHash: String = ""
) {
    val hasPin: Boolean get() = pinHash.isNotBlank()
}
