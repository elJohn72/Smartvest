package com.ajtecnology.smartvest.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val expiresAt: String
)

@Serializable
data class TelemetryReading(
    val deviceId: String,
    val distanceCm: Float? = null,
    val gpsFix: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryLevel: Int? = null,
    val sosActive: Boolean = false,
    val lastUpdate: Long = 0L,
    val bootId: String? = null,
    val sequence: Long = 0L
)

@Serializable
data class TelemetryStateResponse(
    val data: TelemetryReading? = null
)

@Serializable
data class DevicePairRequest(
    val deviceId: String,
    val pairingCode: String
)

@Serializable
data class DevicePairResponse(
    val success: Boolean,
    val deviceId: String
)

@Serializable
data class ShareCreateRequest(
    val allowedFields: List<String>
)

@Serializable
data class ShareCreateResponse(
    val shareId: String,
    val token: String,
    val publicUrl: String
)
