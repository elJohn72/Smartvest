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

fun mapEmbedUrl(latitude: Double, longitude: Double): String {
    val lat = String.format(Locale.US, "%.6f", latitude)
    val lon = String.format(Locale.US, "%.6f", longitude)
    return "https://maps.google.com/maps?q=$lat,$lon&z=16&output=embed"
}

fun mapSearchUrl(latitude: Double, longitude: Double): String {
    val lat = String.format(Locale.US, "%.6f", latitude)
    val lon = String.format(Locale.US, "%.6f", longitude)
    return "https://maps.google.com/?q=$lat,$lon"
}

enum class SosPinKind {
    LIVE,
    LAST,
    CAVANIS
}

data class SosPin(
    val latitude: Double,
    val longitude: Double,
    val kind: SosPinKind
)

const val CavanisLatitude = -0.218470
const val CavanisLongitude = -79.172546

fun placeMapHtml(latitude: Double, longitude: Double, pinColor: String): String {
    val lat = String.format(Locale.US, "%.6f", latitude)
    val lon = String.format(Locale.US, "%.6f", longitude)
    val color = if (pinColor.matches(Regex("#[0-9A-Fa-f]{6}"))) pinColor else "#B91C1C"
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
        <style>
          html, body, #map { height: 100%; margin: 0; background: #F8FAFC; }
          .leaflet-control-attribution { font-size: 10px; background: rgba(248,250,252,.9); }
          .leaflet-bar a { background: #FFFFFF; color: #0F172A; border-bottom-color: #E2E8F0; }
        </style>
        </head>
        <body>
        <div id="map"></div>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
          var map = L.map('map', { zoomControl: true }).setView([$lat, $lon], 16);
          L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap'
          }).addTo(map);
          var icon = L.divIcon({
            className: '',
            html: '<div style="width:18px;height:18px;margin-left:5px;border-radius:50% 50% 50% 0;background:$color;border:3px solid #fff;transform:rotate(-45deg);box-shadow:0 1px 4px rgba(15,23,42,.35)"></div>',
            iconSize: [28, 28],
            iconAnchor: [14, 26]
          });
          L.marker([$lat, $lon], { icon: icon }).addTo(map);
        </script>
        </body>
        </html>
    """.trimIndent()
}

fun resolveSosPin(
    gpsFix: Boolean,
    latitude: Double?,
    longitude: Double?,
    history: List<Triple<Boolean, Double?, Double?>>,
    savedLatitude: Double?,
    savedLongitude: Double?
): SosPin {
    if (hasValidGps(gpsFix, latitude, longitude)) {
        return SosPin(latitude!!, longitude!!, SosPinKind.LIVE)
    }
    val remembered = history.firstOrNull { (fix, lat, lon) -> hasValidGps(fix, lat, lon) }
    if (remembered != null) {
        return SosPin(remembered.second!!, remembered.third!!, SosPinKind.LAST)
    }
    if (hasValidGps(true, savedLatitude, savedLongitude)) {
        return SosPin(savedLatitude!!, savedLongitude!!, SosPinKind.LAST)
    }
    return SosPin(CavanisLatitude, CavanisLongitude, SosPinKind.CAVANIS)
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
