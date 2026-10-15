package com.ajtecnology.smartvest.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ajtecnology.smartvest.data.TelemetryReading
import com.ajtecnology.smartvest.theme.*

/** The WebView loads the bundled engine once; polling moves the marker without navigation. */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun EmbeddedLocationMap(point: MapLocation?, modifier: Modifier = Modifier, color: String = "#1E40AF") {
    val context = LocalContext.current
    val document = remember(context) {
        fun asset(name: String) = context.assets.open("maps/$name").bufferedReader().use { it.readText() }
        locationMapDocument(asset("leaflet.css"), asset("leaflet.js"), asset("location.js"))
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            LocationWebView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.userAgentString = settings.userAgentString + " SmartVest/1.3 (+https://github.com/elJohn72/Smartvest)"
                setBackgroundColor(android.graphics.Color.parseColor("#F8FAFC"))
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        ready = true
                        applyLocation()
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        if (uri.scheme == "https" && uri.host == "www.openstreetmap.org") {
                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: ActivityNotFoundException) { }
                        }
                        return true
                    }
                }
                setOnTouchListener { view, event ->
                    view.parent?.requestDisallowInterceptTouchEvent(
                        event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL
                    )
                    false
                }
                loadDataWithBaseURL("https://smartvest-map.invalid/", document, "text/html", "utf-8", null)
            }
        },
        update = { view -> view.updateLocation(point, color) },
        onRelease = { view -> view.stopLoading(); view.destroy() },
    )
}

private class LocationWebView(context: android.content.Context) : WebView(context) {
    var ready = false
    private var pending: String? = null
    private var applied: String? = null
    fun updateLocation(point: MapLocation?, color: String) {
        pending = if (point == null) "window.smartvestClearLocation();" else locationUpdateScript(point, color)
        applyLocation()
    }
    fun applyLocation() {
        val script = pending ?: return
        if (ready && script != applied) {
            applied = script
            evaluateJavascript(script, null)
        }
    }
}

@Composable
fun LocationMapCard(reading: TelemetryReading) {
    // Keep the last point only for this device and this screen, never across people/sessions.
    var lastPoint by remember(reading.deviceId) { mutableStateOf<MapLocation?>(null) }
    val current = if (hasValidGps(reading.gpsFix, reading.latitude, reading.longitude)) {
        MapLocation(reading.latitude!!, reading.longitude!!)
    } else null
    SideEffect { if (current != null) lastPoint = current }
    val point = current ?: lastPoint
    val context = LocalContext.current
    var unavailable by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ubicación del chaleco", style = MaterialTheme.typography.titleMedium, color = Night)
            if (point == null) {
                Text("Esperando una ubicación GPS válida del chaleco.", color = BodyText)
            } else {
                Text(if (current != null) "Última ubicación recibida" else "Última ubicación recibida · sin fijación GPS actual", color = BodyText)
                Text(gpsLabel(true, point.latitude, point.longitude), color = BodyText)
            }
            // The basemap is visible even before the vest obtains its first GPS fix.
            EmbeddedLocationMap(point, Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp)), if (reading.sosActive) "#B91C1C" else "#1E40AF")
            if (point != null) {
                OutlinedButton(onClick = {
                    val uri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}")
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: ActivityNotFoundException) { unavailable = true }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Abrir en otra aplicación") }
                if (unavailable) Text("No hay una aplicación de mapas disponible.", color = WarnOrange)
            }
        }
    }
}
