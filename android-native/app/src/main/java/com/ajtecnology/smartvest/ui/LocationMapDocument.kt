package com.ajtecnology.smartvest.ui

import java.util.Locale

/** Only coordinates and a validated colour enter JavaScript; never HTML or API strings. */
data class MapLocation(val latitude: Double, val longitude: Double) {
    val valid: Boolean get() = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

fun locationUpdateScript(point: MapLocation, color: String): String? {
    if (!point.valid) return null
    val safeColor = color.takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}")) } ?: "#1E40AF"
    return String.format(Locale.US, "window.smartvestSetLocation(%.6f,%.6f,'%s');", point.latitude, point.longitude, safeColor)
}

fun locationMapDocument(css: String, leaflet: String, controller: String): String = """
<!doctype html><html lang="es"><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src https://tile.openstreetmap.org data:; connect-src 'none'; base-uri 'none'; form-action 'none'">
<style>$css
html {height:100%;width:100%;margin:0}
body {height:100vh;min-height:240px;margin:0;background:#F8FAFC;font:14px sans-serif;color:#0F172A}
body {display:flex;flex-direction:column} #map {flex:1;min-height:0;width:100%}
.toolbar {padding:6px 8px;background:#fff;display:flex;align-items:center;gap:8px;flex-wrap:wrap}
button {min-height:44px;padding:6px 12px;background:#fff;border:1px solid #CBD5E1;border-radius:8px;color:#1E40AF;font:inherit}
button:disabled {color:#64748B;background:#F1F5F9}
#map-status {flex:1;min-width:120px} #map-status:empty {display:none}
.leaflet-control-attribution {font-size:11px} [hidden] {display:none!important}
</style></head><body>
<div id="map" role="region" aria-label="Mapa general. Sin ubicación GPS del chaleco"></div>
<div class="toolbar"><span id="map-status" role="status">Cargando mapa…</span>
<button id="map-retry" hidden>Reintentar</button><button id="map-center" disabled>Centrar ubicación</button></div>
<script>$leaflet</script><script>$controller</script></body></html>
""".trimIndent()
