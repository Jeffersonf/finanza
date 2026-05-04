package com.finanza.v4.data.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore("sync")

class SyncPreferences(context: Context) {
    private val dataStore = context.applicationContext.syncDataStore

    val config: Flow<SyncConfig> = dataStore.data.map { prefs ->
        SyncConfig(
            baseUrl = prefs[BASE_URL].orEmpty(),
            apiKey = prefs[API_KEY].orEmpty(),
            userName = prefs[USER_NAME].orEmpty(),
            autoSyncEnabled = prefs[AUTO_SYNC_ENABLED] ?: false,
            lastSyncAt = prefs[LAST_SYNC_AT] ?: 0L,
            lastSyncMessage = prefs[LAST_SYNC_MESSAGE].orEmpty(),
            lastSyncSuccess = prefs[LAST_SYNC_SUCCESS] ?: false
        )
    }

    suspend fun get(): SyncConfig = config.first()

    suspend fun save(config: SyncConfig) {
        dataStore.edit { prefs ->
            prefs[BASE_URL] = config.baseUrl.trim().trimEnd('/')
            prefs[API_KEY] = config.apiKey.trim()
            prefs[USER_NAME] = config.userName.trim()
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun markSyncResult(message: String, success: Boolean) {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_AT] = System.currentTimeMillis()
            prefs[LAST_SYNC_MESSAGE] = message.trim()
            prefs[LAST_SYNC_SUCCESS] = success
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(API_KEY)
            prefs.remove(USER_NAME)
            prefs[LAST_SYNC_MESSAGE] = "Conta desconectada"
            prefs[LAST_SYNC_AT] = System.currentTimeMillis()
            prefs[LAST_SYNC_SUCCESS] = false
        }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val USER_NAME = stringPreferencesKey("user_name")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_SYNC_MESSAGE = stringPreferencesKey("last_sync_message")
        val LAST_SYNC_SUCCESS = booleanPreferencesKey("last_sync_success")
    }
}

data class SyncConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val userName: String = "",
    val autoSyncEnabled: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncMessage: String = "",
    val lastSyncSuccess: Boolean = false
) {
    val connected: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()
}
