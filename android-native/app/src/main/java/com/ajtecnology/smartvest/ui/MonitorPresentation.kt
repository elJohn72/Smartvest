package com.ajtecnology.smartvest.ui

import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale

enum class DistanceBand {
    DANGER,
    ALERT,
    CAUTION,
    CLEAR,
    UNKNOWN
}

fun distanceBand(distanceCm: Float?): DistanceBand = when {
    distanceCm == null -> DistanceBand.UNKNOWN
    distanceCm <= 40f -> DistanceBand.DANGER
    distanceCm <= 100f -> DistanceBand.ALERT
    distanceCm <= 200f -> DistanceBand.CAUTION
    else -> DistanceBand.CLEAR
}

fun distanceBandLabel(distanceCm: Float?): String = when (distanceBand(distanceCm)) {
    DistanceBand.DANGER -> "Peligro"
    DistanceBand.ALERT -> "Alerta"
    DistanceBand.CAUTION -> "Precaución"
    DistanceBand.CLEAR -> "Despejado"
    DistanceBand.UNKNOWN -> "Sin lectura"
}

fun isReadingStale(nowMs: Long, lastUpdateMs: Long): Boolean {
    if (lastUpdateMs <= 0L) return true
    return (nowMs - lastUpdateMs) > 30_000L
}

fun readingAgeLabel(nowMs: Long, lastUpdateMs: Long): String {
    if (lastUpdateMs <= 0L) return "Sin hora de lectura"
    val seconds = ((nowMs - lastUpdateMs) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 60L -> "hace $seconds s"
        seconds < 3_600L -> "hace ${seconds / 60L} min"
        seconds < 86_400L -> "hace ${seconds / 3_600L} h"
        else -> "hace ${seconds / 86_400L} d"
    }
}

fun batteryLabel(level: Int?): String = if (level == null) "Sin dato" else "$level %"

fun hasValidGps(gpsFix: Boolean, latitude: Double?, longitude: Double?): Boolean {
    if (!gpsFix || latitude == null || longitude == null) return false
    if (latitude.isNaN() || longitude.isNaN()) return false
    return latitude in -90.0..90.0 && longitude in -180.0..180.0
}

fun gpsLabel(gpsFix: Boolean, latitude: Double?, longitude: Double?): String {
    if (!hasValidGps(gpsFix, latitude, longitude)) return "Sin fijación"
    val lat = String.format(Locale.US, "%.5f", latitude)
    val lon = String.format(Locale.US, "%.5f", longitude)
    return "Fijación $lat, $lon"
}

fun parseUtcMillis(recordedAt: String): Long? = try {
    Instant.parse(recordedAt).toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

fun historyAgeLabel(nowMs: Long, recordedAt: String): String {
    val millis = parseUtcMillis(recordedAt) ?: return recordedAt
    return readingAgeLabel(nowMs, millis)
}
