package com.example.cplswellbeingbynovadevs.data.remote

import android.util.Log
import com.example.cplswellbeingbynovadevs.data.model.UpdateManifest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

object UpdateApi {
    private const val TAG = "UpdateApi"
    private const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/skvsmdarman/cpl-wellbeing/main/update.json"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun checkForUpdates(): UpdateManifest? {
        return try {
            val response = SupabaseClient.httpClient.get(UPDATE_MANIFEST_URL)
            if (response.status == HttpStatusCode.OK) {
                val bodyText = response.bodyAsText()
                json.decodeFromString<UpdateManifest>(bodyText)
            } else {
                Log.w(TAG, "Update check HTTP status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates from GitHub", e)
            null
        }
    }
}
