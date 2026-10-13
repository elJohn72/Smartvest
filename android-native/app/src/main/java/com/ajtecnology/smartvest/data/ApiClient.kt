package com.ajtecnology.smartvest.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(
    private val baseUrl: String = "https://smartvest-lab.ajenzasas.com/api/v1",
    private val sessionStore: SessionStore
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(request).toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/auth/login")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val loginResponse = json.decodeFromString<LoginResponse>(respBody)
                sessionStore.saveToken(loginResponse.token)
                Result.success(loginResponse)
            } else {
                Result.failure(IOException("Error HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTelemetryState(deviceId: String): Result<TelemetryReading?> = withContext(Dispatchers.IO) {
        try {
            val token = sessionStore.getToken() ?: return@withContext Result.failure(IllegalStateException("No auth token"))
            val httpRequest = Request.Builder()
                .url("$baseUrl/devices/$deviceId/state")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(httpRequest).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val stateResp = json.decodeFromString<TelemetryStateResponse>(respBody)
                Result.success(stateResp.data)
            } else {
                Result.failure(IOException("Error HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pairDevice(profileId: String, deviceId: String, pairingCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = sessionStore.getToken() ?: return@withContext Result.failure(IllegalStateException("No auth token"))
            val reqDto = DevicePairRequest(deviceId = deviceId, pairingCode = pairingCode)
            val body = json.encodeToString(reqDto).toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/profiles/$profileId/device")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(IOException("Error HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = sessionStore.getToken()
            if (token != null) {
                val httpRequest = Request.Builder()
                    .url("$baseUrl/auth/logout")
                    .header("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(jsonMediaType))
                    .build()
                client.newCall(httpRequest).execute()
            }
            sessionStore.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionStore.clearSession()
            Result.success(Unit)
        }
    }
}
