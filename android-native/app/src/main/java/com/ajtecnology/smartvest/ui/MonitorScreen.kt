package com.ajtecnology.smartvest.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.ProfileDto
import com.ajtecnology.smartvest.data.TelemetryReading
import com.ajtecnology.smartvest.theme.BodyText
import com.ajtecnology.smartvest.theme.CardBg
import com.ajtecnology.smartvest.theme.CautionSoft
import com.ajtecnology.smartvest.theme.ClearGreen
import com.ajtecnology.smartvest.theme.Inset
import com.ajtecnology.smartvest.theme.MetaText
import com.ajtecnology.smartvest.theme.Night
import com.ajtecnology.smartvest.theme.Outline
import com.ajtecnology.smartvest.theme.Paper
import com.ajtecnology.smartvest.theme.Signal
import com.ajtecnology.smartvest.theme.SosRed
import com.ajtecnology.smartvest.theme.SosSoft
import com.ajtecnology.smartvest.theme.WarnOrange
import com.ajtecnology.smartvest.theme.WarnSoft
import kotlinx.coroutines.delay

@Composable
fun MonitorScreen(
    viewModel: MainViewModel,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.monitorState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val history by viewModel.history.collectAsState()
    val qrShareData by viewModel.qrShareData.collectAsState()

    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPairDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showInternetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var destination by rememberSaveable { mutableIntStateOf(0) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            NavigationBar(containerColor = CardBg, tonalElevation = 0.dp) {
                val labels = listOf(R.string.tab_monitor, R.string.tab_profiles, R.string.tab_history)
                val icons = listOf(Icons.Filled.Sensors, Icons.Filled.People, Icons.Filled.History)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = destination == index,
                        onClick = { destination = index },
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }
    ) { insets ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Image(painterResource(R.mipmap.ic_smartvest), contentDescription = null, modifier = Modifier.size(36.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge,
                        color = Night, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    IconButton(onClick = onLogoutClick, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout_button), tint = MetaText)
                    }
                }
                Text(stringResource(R.string.lab_short), style = MaterialTheme.typography.bodyMedium, color = MetaText,
                    modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Text(stringResource(when (destination) {
                    1 -> R.string.tab_profiles
                    2 -> R.string.tab_history
                    else -> R.string.monitor_title
                }), style = MaterialTheme.typography.headlineMedium, color = Night, modifier = Modifier.semantics { heading() })
                Text(stringResource(when (destination) {
                    1 -> R.string.profiles_subtitle
                    2 -> R.string.history_subtitle
                    else -> R.string.monitor_subtitle
                }), style = MaterialTheme.typography.bodyLarge, color = BodyText,
                    modifier = Modifier.padding(top = 8.dp))
            }
            when (destination) {
                0 -> {
                    if (selectedProfile == null) {
                        item {
                            SetupCard(stringResource(R.string.no_profile_title), stringResource(R.string.no_profile_body),
                                stringResource(R.string.new_profile)) { showCreateProfileDialog = true }
                        }
                    } else {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Inset)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(stringResource(R.string.selected_person), style = MaterialTheme.typography.bodyMedium, color = MetaText)
                                    Text(selectedProfile!!.presentationName, style = MaterialTheme.typography.titleMedium, color = Night)
                                    TextButton(onClick = { destination = 1 }) { Text(stringResource(R.string.profile_switch)) }
                                }
                            }
                        }
                        if (selectedProfile?.deviceId.isNullOrBlank()) {
                            item {
                                SetupCard(stringResource(R.string.vest_setup_title), stringResource(R.string.vest_setup_body),
                                    stringResource(R.string.pair_button)) { showPairDialog = true }
                            }
                        } else {
                            item { MonitorStatus(state, true, nowMs) }
                        }
                        item {
                            OutlinedButton(onClick = { showInternetDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                Text(stringResource(R.string.vest_internet_button))
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showQrDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                Text(stringResource(R.string.qr_button))
                            }
                        }
                    }
                    item {
                        Text(stringResource(R.string.foreground_only_banner), style = MaterialTheme.typography.bodyMedium, color = BodyText)
                        Text(
                            stringResource(R.string.lab_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetaText,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                1 -> {
                    if (profiles.isEmpty()) {
                        item {
                            SetupCard(
                                stringResource(R.string.no_profile_title),
                                stringResource(R.string.no_profile_body),
                                stringResource(R.string.new_profile)
                            ) { showCreateProfileDialog = true }
                        }
                    } else {
                        item {
                            WideButton(stringResource(R.string.new_profile), Signal) { showCreateProfileDialog = true }
                        }
                        items(profiles, key = { "profile-${it.id}" }) { profile ->
                            ProfileCard(
                                profile = profile,
                                isSelected = profile.id == selectedProfile?.id,
                                onClick = { viewModel.selectProfile(profile) }
                            )
                        }
                        selectedProfile?.let { profile ->
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (profile.deviceId.isNullOrBlank()) {
                                        WideButton(stringResource(R.string.pair_button), Signal) { showPairDialog = true }
                                    }
                                    WideButton(stringResource(R.string.edit_profile), Signal, outlined = true) {
                                        showEditProfileDialog = true
                                    }
                                    WideButton(stringResource(R.string.qr_button), Signal, outlined = true) { showQrDialog = true }
                                    TextButton(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text(stringResource(R.string.remove_profile), color = SosRed, fontSize = 16.sp)
                                    }
                                    Text(stringResource(R.string.profile_private_note), color = MetaText, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (selectedProfile != null) {
                        item { Text(selectedProfile!!.presentationName, style = MaterialTheme.typography.titleMedium, color = Signal) }
                    }
                    if (history.isEmpty()) {
                        item {
                            SetupCard(
                                stringResource(R.string.history_title),
                                stringResource(R.string.history_empty),
                                stringResource(R.string.tab_monitor)
                            ) { destination = 0 }
                        }
                    }
        items(history, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = distanceBandLabel(item.distanceCm) + ": " + (item.distanceCm?.toInt()?.toString() ?: "--") + " cm",
                        fontWeight = FontWeight.SemiBold,
                        color = BodyText,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (item.sosActive) stringResource(R.string.sos_active_alert) else stringResource(R.string.history_sos_clear),
                        color = if (item.sosActive) SosRed else MetaText,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.battery_label) + ": " + batteryLabel(item.batteryLevel),
                        color = BodyText,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.reading_age_label) + ": " + historyAgeLabel(nowMs, item.recordedAt),
                        color = MetaText,
                        fontSize = 14.sp
                    )
                }
            }
        }
                }
            }
        }
    }

    if (showCreateProfileDialog) {
        ProfileFormDialog(
            title = stringResource(R.string.create_profile_title),
            initial = null,
            onDismiss = { showCreateProfileDialog = false },
            onSave = { name, phone, blood, notes, onDone ->
                viewModel.createProfile(name, phone, blood, notes, onDone)
            }
        )
    }
    if (showEditProfileDialog) {
        ProfileFormDialog(
            title = stringResource(R.string.edit_profile_title),
            initial = selectedProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, blood, notes, onDone ->
                viewModel.updateProfile(name, phone, blood, notes, onDone)
            }
        )
    }
    if (showPairDialog) {
        PairDeviceDialog(
            onDismiss = { showPairDialog = false },
            onPair = { deviceId, code, onDone -> viewModel.pairDevice(deviceId, code, onDone) }
        )
    }
    if (showInternetDialog) {
        VestInternetDialog(onDismiss = { showInternetDialog = false })
    }
    if (showQrDialog && selectedProfile != null) {
        QrShareDialog(
            share = qrShareData,
            onDismiss = { showQrDialog = false },
            onGenerate = { fields, onDone ->
                viewModel.generateQrShare(selectedProfile!!.id, fields, onDone)
            },
            onRevoke = { shareId, onDone ->
                viewModel.revokeQrShare(selectedProfile!!.id, shareId, onDone)
            }
        )
    }
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { onDone -> viewModel.deleteProfile(onDone) }
        )
    }
}

@Composable
private fun ProfileCard(profile: ProfileDto, isSelected: Boolean, onClick: () -> Unit) {
    val vest = if (profile.deviceId.isNullOrBlank()) {
        stringResource(R.string.vest_unlinked)
    } else {
        stringResource(R.string.vest_linked, profile.deviceId)
    }
    val spoken = if (isSelected) {
        stringResource(R.string.selected_profile, profile.presentationName) + ". " + vest
    } else {
        profile.presentationName + ". " + vest
    }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                selected = isSelected
                contentDescription = spoken
            },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Signal else Outline),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Inset else CardBg)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isSelected) {
                Text(stringResource(R.string.profile_card_selected), style = MaterialTheme.typography.bodyMedium, color = Signal)
            }
            Text(profile.presentationName, style = MaterialTheme.typography.titleMedium, color = Night)
            Text(vest, color = if (profile.deviceId.isNullOrBlank()) WarnOrange else ClearGreen, style = MaterialTheme.typography.bodyLarge)
            profile.emergencyPhone?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.profile_phone_label) + ": " + it, color = BodyText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SetupCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Night)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = BodyText)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(action) }
        }
    }
}

@Composable
private fun NoticeCard(text: String, background: Color, content: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Text(text = text, color = content, fontSize = 16.sp, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun WideButton(
    label: String,
    color: Color,
    outlined: Boolean = false,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label, fontSize = 16.sp) }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) { Text(label, fontSize = 16.sp) }
    }
}

@Composable
private fun MonitorStatus(state: MonitorUiState, hasVest: Boolean, nowMs: Long) {
    when (state) {
        is MonitorUiState.Loading -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = Signal)
                Text(stringResource(R.string.status_loading), color = MetaText, fontSize = 16.sp)
            }
        }
        is MonitorUiState.SessionExpired -> {
            NoticeCard(stringResource(R.string.status_no_connection), SosSoft, SosRed)
        }
        is MonitorUiState.NoData -> {
            NoticeCard(
                if (hasVest) stringResource(R.string.status_no_data) else stringResource(R.string.status_no_vest),
                Inset,
                BodyText
            )
        }
        is MonitorUiState.NoConnection -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoticeCard(stringResource(R.string.status_no_connection), WarnSoft, WarnOrange)
                state.lastReading?.let { reading ->
                    Text(stringResource(R.string.last_reading_kept), color = MetaText, fontSize = 14.sp)
                    ReadingCard(reading, nowMs)
                }
            }
        }
        is MonitorUiState.Ready -> ReadingCard(state.reading, nowMs)
    }
}

@Composable
private fun ReadingCard(reading: TelemetryReading, nowMs: Long) {
    val stale = isReadingStale(nowMs, reading.lastUpdate)
    val bandColor = when (distanceBand(reading.distanceCm)) {
        DistanceBand.DANGER -> SosRed
        DistanceBand.ALERT -> WarnOrange
        DistanceBand.CAUTION -> WarnOrange
        DistanceBand.CLEAR -> ClearGreen
        DistanceBand.UNKNOWN -> MetaText
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (reading.sosActive) {
            Card(colors = CardDefaults.cardColors(containerColor = SosSoft), modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.sos_active_alert), color = SosRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(stringResource(R.string.sos_detail), color = BodyText, fontSize = 16.sp)
                }
            }
        } else {
            Text(stringResource(R.string.sos_inactive), color = BodyText, fontSize = 16.sp)
        }
        if (stale) {
            NoticeCard(stringResource(R.string.status_stale), CautionSoft, WarnOrange)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.reading_section), style = MaterialTheme.typography.bodyMedium, color = MetaText)
                Text(distanceBandLabel(reading.distanceCm), color = bandColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = reading.distanceCm?.toInt()?.toString() ?: "--",
                    fontSize = 56.sp,
                    lineHeight = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = bandColor
                )
                Text(stringResource(R.string.distance_unit), color = MetaText, fontSize = 16.sp)
                Text(
                    stringResource(R.string.battery_label) + ": " + batteryLabel(reading.batteryLevel),
                    color = BodyText,
                    fontSize = 16.sp
                )
                Text(
                    stringResource(R.string.gps_label) + ": " + gpsLabel(reading.gpsFix, reading.latitude, reading.longitude),
                    color = BodyText,
                    fontSize = 16.sp
                )
                Text(
                    stringResource(R.string.reading_age_label) + ": " + readingAgeLabel(nowMs, reading.lastUpdate),
                    color = BodyText,
                    fontSize = 16.sp
                )
                if (hasValidGps(reading.gpsFix, reading.latitude, reading.longitude)) {
                    MapButton(reading.latitude!!, reading.longitude!!)
                }
            }
        }
    }
}

@Composable
private fun MapButton(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    var unavailable by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                unavailable = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(stringResource(R.string.open_map), fontSize = 16.sp)
    }
    if (unavailable) {
        Text(stringResource(R.string.map_unavailable), color = WarnOrange, fontSize = 14.sp)
    }
}
