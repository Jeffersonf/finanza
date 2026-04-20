package com.finanza.v4.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore("sync")

class SyncPreferences(context: Context) {
    private val dataStore = context.applicationContext.syncDataStore

    val config: Flow<SyncConfig> = dataStore.data.map { prefs ->
        SyncConfig(
            baseUrl = prefs[BASE_URL].orEmpty(),
            apiKey = prefs[API_KEY].orEmpty(),
            userName = prefs[USER_NAME].orEmpty()
        )
    }

    suspend fun save(config: SyncConfig) {
        dataStore.edit { prefs ->
            prefs[BASE_URL] = config.baseUrl.trim().trimEnd('/')
            prefs[API_KEY] = config.apiKey.trim()
            prefs[USER_NAME] = config.userName.trim()
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(API_KEY)
            prefs.remove(USER_NAME)
        }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val USER_NAME = stringPreferencesKey("user_name")
    }
}

data class SyncConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val userName: String = ""
) {
    val connected: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()
}
