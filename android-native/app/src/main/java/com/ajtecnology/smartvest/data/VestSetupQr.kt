package com.ajtecnology.smartvest.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Etiqueta de alta del chaleco. Mismos campos que el aprovisionamiento
 * por QR de una torre: versión, nombre Bluetooth, posesión y transporte.
 * No lleva la clave del Wi-Fi ni datos de la persona.
 */
@Serializable
data class VestSetupQr(
    val ver: String,
    val name: String,
    val pop: String,
    val transport: String,
)

private val setupJson = Json { ignoreUnknownKeys = true }

fun parseVestSetupQr(raw: String): VestSetupQr? {
    if (raw.length > 1024) return null
    val trimmed = raw.trim()
    if (trimmed.length > 1024 || !trimmed.startsWith("{")) return null
    val parsed = try {
        setupJson.decodeFromString<VestSetupQr>(trimmed)
    } catch (_: Exception) {
        return null
    }
    val pop = parsed.pop
    if (parsed.ver != "v1") return null
    if (!Regex("PROV_SmartVest_[0-9A-F]{4}").matches(parsed.name)) return null
    if (!Regex("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}").matches(pop)) return null
    if (parsed.transport != "ble") return null
    return parsed
}
