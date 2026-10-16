package com.ajtecnology.smartvest.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.AssistantAnalytics
import com.ajtecnology.smartvest.data.AssistantChatMessage
import com.ajtecnology.smartvest.data.AssistantPlace
import com.ajtecnology.smartvest.data.AssistantSosEvent
import com.ajtecnology.smartvest.data.ProfileDto
import com.ajtecnology.smartvest.theme.BodyText
import com.ajtecnology.smartvest.theme.CardBg
import com.ajtecnology.smartvest.theme.CautionSoft
import com.ajtecnology.smartvest.theme.CautionYellow
import com.ajtecnology.smartvest.theme.ClearGreen
import com.ajtecnology.smartvest.theme.ClearSoft
import com.ajtecnology.smartvest.theme.Inset
import com.ajtecnology.smartvest.theme.MetaText
import com.ajtecnology.smartvest.theme.Night
import com.ajtecnology.smartvest.theme.Outline
import com.ajtecnology.smartvest.theme.Signal
import com.ajtecnology.smartvest.theme.SosRed
import com.ajtecnology.smartvest.theme.SosSoft
import com.ajtecnology.smartvest.theme.WarnOrange
import com.ajtecnology.smartvest.theme.WarnSoft
import java.text.DateFormat
import java.util.Date

private val quickPrompts = listOf(
    "¿Cómo se encuentra?",
    "¿El chaleco está en línea?",
    "¿Cuál fue su última ubicación?",
    "¿Hubo pulsaciones SOS recientes?",
    "¿Qué tan riesgoso es el día?"
)

@Composable
fun AssistantPanel(
    profile: ProfileDto?,
    viewModel: MainViewModel,
    nowMs: Long,
    modifier: Modifier = Modifier
) {
    val deviceId = profile?.deviceId
    var tab by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var analytics by remember { mutableStateOf<AssistantAnalytics?>(null) }
    var insights by remember { mutableStateOf("") }
    var aiPowered by remember { mutableStateOf(false) }
    var aiModel by remember { mutableStateOf<String?>(null) }
    var messages by remember {
        mutableStateOf(
            listOf(
                AssistantChatMessage(
                    role = "assistant",
                    content = "Hola, soy el asistente SmartVest. Puedo ayudarte con el estado de la persona, la ubicación, el SOS y el nivel de riesgo según las lecturas del chaleco."
                )
            )
        )
    }
    var draft by remember { mutableStateOf("") }
    var chatLoading by remember { mutableStateOf(false) }
    val failedText = stringResource(R.string.assistant_failed)

    fun reload() {
        val id = deviceId ?: return
        loading = true
        error = null
        viewModel.loadAssistant(id) { result ->
            loading = false
            result.fold(
                onSuccess = { body ->
                    analytics = body.analytics
                    insights = body.insights.orEmpty()
                    aiPowered = body.aiPowered
                    aiModel = body.aiModel
                },
                onFailure = { error = failedText }
            )
        }
    }

    LaunchedEffect(deviceId) {
        if (!deviceId.isNullOrBlank()) reload()
    }

    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.assistant_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Night,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    profile?.presentationName ?: stringResource(R.string.assistant_no_profile),
                    style = MaterialTheme.typography.bodyLarge,
                    color = BodyText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = { reload() }, enabled = !loading && !deviceId.isNullOrBlank(), modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.assistant_refresh), tint = Signal)
            }
        }

        if (deviceId.isNullOrBlank()) {
            Text(
                stringResource(R.string.assistant_needs_vest),
                color = BodyText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp)
            )
            return
        }

        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Inset)
                .padding(4.dp)
        ) {
            AssistantTab(stringResource(R.string.assistant_tab_summary), tab == 0, Modifier.weight(1f)) { tab = 0 }
            AssistantTab(stringResource(R.string.assistant_tab_chat), tab == 1, Modifier.weight(1f)) { tab = 1 }
        }

        if (tab == 0) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (loading && analytics == null) {
                    CircularProgressIndicator(color = Signal, modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally))
                    Text(stringResource(R.string.assistant_loading), color = MetaText, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                error?.let {
                    Text(it, color = SosRed, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                analytics?.let { data ->
                    SummaryCards(data, insights, aiPowered, aiModel, nowMs)
                }
                Spacer(Modifier.height(24.dp))
            }
        } else {
            ChatPane(
                messages = messages,
                draft = draft,
                loading = chatLoading,
                onDraft = { draft = it },
                onSend = { text ->
                    val trimmed = text.trim()
                    if (trimmed.isEmpty() || chatLoading) return@ChatPane
                    val next = messages + AssistantChatMessage("user", trimmed)
                    messages = next
                    draft = ""
                    chatLoading = true
                    viewModel.sendAssistantChat(deviceId, next) { result ->
                        chatLoading = false
                        result.fold(
                            onSuccess = { body ->
                                body.analytics?.let { analytics = it }
                                aiPowered = body.aiPowered
                                aiModel = body.aiModel
                                messages = next + AssistantChatMessage("assistant", body.reply ?: failedText)
                            },
                            onFailure = {
                                messages = next + AssistantChatMessage("assistant", failedText)
                            }
                        )
                    }
                },
                modifier = Modifier.weight(1f).padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun AssistantTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) CardBg else Color.Transparent,
            contentColor = if (selected) Night else BodyText
        )
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryCards(
    data: AssistantAnalytics,
    insights: String,
    aiPowered: Boolean,
    aiModel: String?,
    nowMs: Long
) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RiskGauge(data.riskScore, data.dailyRiskLevel)
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.assistant_device_line, connectionLabel(data)), color = BodyText)
                Text(
                    stringResource(
                        R.string.assistant_avg_distance,
                        data.avgDistanceCm?.let { "${it.toInt()} cm" } ?: stringResource(R.string.assistant_no_readings)
                    ),
                    color = BodyText
                )
                Text(
                    stringResource(R.string.assistant_danger_ratio, (data.dangerRatio * 100).let { "%.1f".format(it) }),
                    color = BodyText
                )
                Text(stringResource(R.string.assistant_samples, data.historyPoints), color = BodyText)
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.assistant_current), style = MaterialTheme.typography.titleMedium, color = Night)
            Text(
                stringResource(if (data.sosActiveNow) R.string.assistant_sos_now else R.string.assistant_sos_idle),
                color = if (data.sosActiveNow) SosRed else ClearGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { liveRegion = if (data.sosActiveNow) LiveRegionMode.Assertive else LiveRegionMode.Polite }
            )
            Text(stringResource(R.string.assistant_sos_count, data.sosCount), color = BodyText)
            data.lastLocation?.let { location ->
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(location.mapsUrl)))
                }) {
                    Text(stringResource(R.string.assistant_open_map) + " · " + relativeTime(nowMs, location.lastUpdate))
                }
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.assistant_places), style = MaterialTheme.typography.titleMedium, color = Night)
            if (data.frequentPlaces.isEmpty()) {
                Text(stringResource(R.string.assistant_places_empty), color = MetaText)
            } else {
                data.frequentPlaces.forEach { place ->
                    PlaceRow(place, nowMs) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(place.mapsUrl)))
                    }
                }
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.assistant_sos_list), style = MaterialTheme.typography.titleMedium, color = Night)
            if (data.sosEvents.isEmpty()) {
                Text(stringResource(R.string.assistant_sos_empty), color = MetaText)
            } else {
                data.sosEvents.take(8).forEach { event ->
                    SosRow(event) {
                        val lat = event.latitude
                        val lng = event.longitude
                        if (lat != null && lng != null && (kotlin.math.abs(lat) > 0.0001 || kotlin.math.abs(lng) > 0.0001)) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                            )
                        }
                    }
                }
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = Inset), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.assistant_analysis), style = MaterialTheme.typography.titleMedium, color = Night, modifier = Modifier.weight(1f))
                Text(
                    if (aiPowered) stringResource(R.string.assistant_badge_ai, aiModel ?: "Groq") else stringResource(R.string.assistant_badge_stats),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (aiPowered) Signal else MetaText)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            InsightText(insights)
        }
    }
}

@Composable
private fun RiskGauge(score: Int, level: String) {
    val (fg, bg) = riskColors(level)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(bg)
                .border(4.dp, fg, CircleShape)
                .semantics { contentDescription = "Riesgo ${riskLabel(level)}, puntuación $score de 100" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                score.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = Night,
                modifier = Modifier.semantics { }
            )
        }
        Text(
            stringResource(R.string.assistant_risk, riskLabel(level)),
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PlaceRow(place: AssistantPlace, nowMs: Long, onMap: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Inset)
            .padding(12.dp)
    ) {
        Text("Zona ${"%.4f".format(place.latitude)}, ${"%.4f".format(place.longitude)}", color = Night, fontWeight = FontWeight.SemiBold)
        Text(
            "${place.visitCount} lecturas · ${place.avgDistanceCm?.let { "${it.toInt()} cm" } ?: "—"} · ${relativeTime(nowMs, place.lastSeenAt)}",
            color = MetaText
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.assistant_risk, riskLabel(place.riskLevel)), color = riskColors(place.riskLevel).first, fontWeight = FontWeight.Bold)
            TextButton(onClick = onMap) { Text(stringResource(R.string.assistant_map)) }
        }
    }
}

@Composable
private fun SosRow(event: AssistantSosEvent, onMap: () -> Unit) {
    val whenText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.at))
    val distance = event.distanceCm?.takeIf { it < 9000 }?.let { " · ${it.toInt()} cm" }.orEmpty()
    TextButton(onClick = onMap, modifier = Modifier.fillMaxWidth()) {
        Text("$whenText$distance", color = SosRed)
    }
}

@Composable
private fun InsightText(text: String) {
    val blocks = text.split(Regex("(?=^## )", RegexOption.MULTILINE)).filter { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (blocks.isEmpty()) {
            Text(text, color = BodyText)
            return
        }
        blocks.forEach { block ->
            val lines = block.trim().lines()
            val title = lines.firstOrNull()?.removePrefix("## ")?.trim().orEmpty()
            val body = lines.drop(1).joinToString("\n").trim()
            if (title.isNotEmpty()) {
                Text(title, color = Night, fontWeight = FontWeight.Bold)
            }
            if (body.isNotEmpty()) {
                Text(boldSpans(body), color = BodyText)
            }
        }
    }
}

private fun boldSpans(source: String) = buildAnnotatedString {
    val parts = source.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Night)) { append(part) }
        } else {
            append(part)
        }
    }
}

@Composable
private fun ChatPane(
    messages: List<AssistantChatMessage>,
    draft: String,
    loading: Boolean,
    onDraft: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transcript = rememberScrollState()
    LaunchedEffect(messages.size, loading) {
        transcript.animateScrollTo(transcript.maxValue)
    }
    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(transcript),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.forEach { message ->
                val mine = message.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Text(
                        message.content,
                        color = if (mine) Color.White else BodyText,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (mine) Signal else Inset)
                            .padding(14.dp)
                    )
                }
            }
            if (loading) {
                Text(
                    stringResource(R.string.assistant_thinking),
                    color = MetaText,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickPrompts.forEach { prompt ->
                TextButton(onClick = { onSend(prompt) }, enabled = !loading) {
                    Text(prompt, color = Signal, maxLines = 1)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.assistant_placeholder), color = MetaText) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Night),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Night,
                    unfocusedTextColor = Night,
                    cursorColor = Signal,
                    focusedBorderColor = Signal,
                    unfocusedBorderColor = Outline,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend(draft) }),
                singleLine = true,
                enabled = !loading
            )
            Button(
                onClick = { onSend(draft) },
                enabled = !loading && draft.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp).heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Signal)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.assistant_send))
            }
        }
    }
}

private fun riskLabel(level: String): String = when (level) {
    "medio" -> "medio"
    "alto" -> "alto"
    "critico" -> "crítico"
    else -> "bajo"
}

private fun riskColors(level: String): Pair<Color, Color> = when (level) {
    "medio" -> CautionYellow to CautionSoft
    "alto" -> WarnOrange to WarnSoft
    "critico" -> SosRed to SosSoft
    else -> ClearGreen to ClearSoft
}

@Composable
private fun connectionLabel(data: AssistantAnalytics): String = when {
    data.deviceOnline -> stringResource(R.string.assistant_online)
    data.connectionStatus == "stale" -> stringResource(R.string.assistant_stale)
    else -> stringResource(R.string.assistant_offline)
}

private fun relativeTime(nowMs: Long, thenMs: Long): String {
    if (thenMs <= 0L) return "—"
    val minutes = ((nowMs - thenMs) / 60000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> "ahora"
        minutes < 60L -> "hace $minutes min"
        else -> "hace ${minutes / 60L} h"
    }
}
