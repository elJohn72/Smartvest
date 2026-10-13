package com.ajtecnology.smartvest.data

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val id: String,
    val username: String
)

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
data class ProfileDto(
    val id: String,
    val presentationName: String,
    val emergencyPhone: String? = null,
    val bloodType: String? = null,
    val medicalObservations: String? = null,
    val deviceId: String? = null
)

@Serializable
data class ProfileListResponse(
    val data: List<ProfileDto>
)

@Serializable
data class ProfileCreateRequest(
    val presentationName: String,
    val emergencyPhone: String? = null,
    val bloodType: String? = null,
    val medicalObservations: String? = null
)

@Serializable
data class ProfileCreateResponse(
    val id: String,
    val presentationName: String
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
data class TelemetryHistoryItem(
    val id: Long,
    val deviceId: String,
    val distanceCm: Float? = null,
    val gpsFix: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryLevel: Int? = null,
    val sosActive: Boolean = false,
    val recordedAt: String
)

@Serializable
data class TelemetryHistoryResponse(
    val data: List<TelemetryHistoryItem>
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
