package com.finanza.v4.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class FinanzaApiClient(
    private val http: OkHttpClient = OkHttpClient()
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(baseUrl: String, username: String, password: String): SyncConfig = withContext(Dispatchers.IO) {
        val cleanBaseUrl = baseUrl.trim().trimEnd('/')
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody(jsonType)
        val request = Request.Builder()
            .url("$cleanBaseUrl/api/login")
            .post(body)
            .build()
        val response = http.newCall(request).execute()
        val text = response.body.string()
        if (!response.isSuccessful) error(readError(text, "Login falhou"))
        val json = JSONObject(text)
        SyncConfig(
            baseUrl = cleanBaseUrl,
            apiKey = json.getString("api_key"),
            userName = json.optString("name", username)
        )
    }

    suspend fun putImport(config: SyncConfig, backupJson: JSONObject) = withContext(Dispatchers.IO) {
        val request = authorized(config, "/api/import")
            .put(backupJson.toString().toRequestBody(jsonType))
            .build()
        executeUnit(request, "Falha ao enviar dados")
    }

    suspend fun getState(config: SyncConfig): JSONObject = getJson(config, "/api/state")

    suspend fun getTransactions(config: SyncConfig): JSONObject = getJson(config, "/api/transactions?limit=1000")

    suspend fun getBudgets(config: SyncConfig): String = withContext(Dispatchers.IO) {
        val request = authorized(config, "/api/budgets").get().build()
        val response = http.newCall(request).execute()
        val text = response.body.string()
        if (!response.isSuccessful) error(readError(text, "Falha ao baixar orçamentos"))
        text
    }

    suspend fun getGoals(config: SyncConfig): String = withContext(Dispatchers.IO) {
        val request = authorized(config, "/api/goals").get().build()
        val response = http.newCall(request).execute()
        val text = response.body.string()
        if (!response.isSuccessful) error(readError(text, "Falha ao baixar metas"))
        text
    }

    private suspend fun getJson(config: SyncConfig, path: String): JSONObject = withContext(Dispatchers.IO) {
        val request = authorized(config, path).get().build()
        val response = http.newCall(request).execute()
        val text = response.body.string()
        if (!response.isSuccessful) error(readError(text, "Falha na sincronização"))
        JSONObject(text)
    }

    private fun authorized(config: SyncConfig, path: String): Request.Builder {
        return Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}$path")
            .header("x-api-key", config.apiKey)
    }

    private fun executeUnit(request: Request, fallback: String) {
        val response = http.newCall(request).execute()
        val text = response.body.string()
        if (!response.isSuccessful) error(readError(text, fallback))
    }

    private fun readError(text: String, fallback: String): String {
        return runCatching { JSONObject(text).optString("error").ifBlank { fallback } }.getOrDefault(fallback)
    }
}
