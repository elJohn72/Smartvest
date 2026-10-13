package com.ajtecnology.smartvest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.ProfileDto
import com.ajtecnology.smartvest.theme.*

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
    var showPairDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Cabecera principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Night
                )
                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Night)
                ) {
                    Text(stringResource(R.string.logout_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Perfiles del Cuidador
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Persona Asistida",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Night
                        )
                        TextButton(onClick = { showCreateProfileDialog = true }) {
                            Text("+ Nuevo Perfil", color = Signal)
                        }
                    }

                    if (profiles.isEmpty()) {
                        Text(
                            text = "No hay perfiles creados. Pulsa '+ Nuevo Perfil' para comenzar.",
                            color = MetaText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profiles.forEach { p ->
                                val isSelected = p.id == selectedProfile?.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectProfile(p) },
                                    label = { Text(p.presentationName) }
                                )
                            }
                        }
                    }

                    // Información del chaleco asignado al perfil
                    selectedProfile?.let { p ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chaleco: ${p.deviceId ?: "No vinculado"}",
                                color = if (p.deviceId != null) ClearGreen else WarnOrange,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Row {
                                if (p.deviceId == null) {
                                    OutlinedButton(onClick = { showPairDialog = true }) {
                                        Text("Vincular", fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.generateQrShare(p.id)
                                        showQrDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Signal)
                                ) {
                                    Text("QR Emergencia", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Estado del Monitoreo
        item {
            when (val s = state) {
                is MonitorUiState.Loading -> {
                    CircularProgressIndicator(color = Signal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.status_loading), color = MetaText)
                }
                is MonitorUiState.SessionExpired -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SosSoft)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Sesión Expirada", fontWeight = FontWeight.Bold, color = SosRed)
                            Text("Tu sesión ha expirado por seguridad. Por favor vuelve a iniciar sesión.", color = BodyText, fontSize = 13.sp)
                        }
                    }
                }
                is MonitorUiState.NoData -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Inset)
                    ) {
                        Text(
                            text = if (selectedProfile?.deviceId == null) "Vincula un chaleco al perfil para recibir telemetría." else stringResource(R.string.status_no_data),
                            modifier = Modifier.padding(24.dp),
                            color = BodyText
                        )
                    }
                }
                is MonitorUiState.NoConnection -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WarnSoft)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.status_no_connection),
                                fontWeight = FontWeight.Bold,
                                color = WarnOrange
                            )
                            Text(text = s.message, color = BodyText, fontSize = 12.sp)
                        }
                    }
                }
                is MonitorUiState.Ready -> {
                    val r = s.reading

                    // Banner SOS
                    if (r.sosActive) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SosSoft)
                        ) {
                            Text(
                                text = stringResource(R.string.sos_active_alert),
                                color = SosRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Banner Stale
                    if (s.isStale) {
                        Text(
                            text = stringResource(R.string.status_stale),
                            color = CautionYellow,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Lectura Principal
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val dist = r.distanceCm
                            if (dist != null) {
                                Text(
                                    text = "${dist.toInt()}",
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dist <= 40) SosRed else if (dist <= 100) WarnOrange else ClearGreen
                                )
                                Text(
                                    text = stringResource(R.string.distance_unit),
                                    fontSize = 16.sp,
                                    color = MetaText
                                )
                            } else {
                                Text("Sin lectura de distancia", color = MetaText)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Text(
                                    text = "${stringResource(R.string.battery_label)} ${r.batteryLevel ?: "--"}%",
                                    color = BodyText
                                )
                                Text(
                                    text = if (r.gpsFix) stringResource(R.string.gps_fix) else stringResource(R.string.gps_no_fix),
                                    color = if (r.gpsFix) ClearGreen else MetaText
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Historial de Telemetría
        if (history.isNotEmpty()) {
            item {
                Text(
                    text = "Historial Reciente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Night,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            items(history) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Distancia: ${item.distanceCm?.toInt() ?: "--"} cm",
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.sosActive) SosRed else BodyText
                            )
                            Text(text = item.recordedAt, fontSize = 11.sp, color = MetaText)
                        }
                        if (item.sosActive) {
                            Text("SOS", color = SosRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal Crear Perfil
    if (showCreateProfileDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var blood by remember { mutableStateOf("") }
        var obs by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            title = { Text("Crear Perfil Asistido") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre (obligatorio)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono de emergencia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = blood,
                        onValueChange = { blood = it },
                        label = { Text("Grupo sanguíneo (ej: O+)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = obs,
                        onValueChange = { obs = it },
                        label = { Text("Observaciones médicas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createProfile(name.trim(), phone.trim(), blood.trim(), obs.trim()) {
                                showCreateProfileDialog = false
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal Vincular Chaleco (Requiere Device ID real y código de un solo uso)
    if (showPairDialog) {
        var inputDeviceId by remember { mutableStateOf(selectedProfile?.deviceId ?: "") }
        var pairingCode by remember { mutableStateOf("") }
        var isPairing by remember { mutableStateOf(false) }
        var pairError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            title = { Text("Vincular Chaleco") },
            text = {
                Column {
                    Text(
                        text = "Introduce el identificador del chaleco y el código de emparejamiento temporal generado en el dispositivo:",
                        fontSize = 13.sp,
                        color = BodyText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputDeviceId,
                        onValueChange = { inputDeviceId = it },
                        label = { Text("ID del Dispositivo (ej. VEST-LAB-01)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it },
                        label = { Text("Código de emparejamiento (OTP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (pairError != null) {
                        Text(pairError!!, color = SosRed, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPairing = true
                        pairError = null
                        viewModel.pairDevice(inputDeviceId.trim(), pairingCode.trim()) { success ->
                            isPairing = false
                            if (success) {
                                showPairDialog = false
                            } else {
                                pairError = "Identificador o código inválido, expirado o ya vinculado."
                            }
                        }
                    },
                    enabled = !isPairing && inputDeviceId.isNotBlank() && pairingCode.isNotBlank()
                ) {
                    Text(if (isPairing) "Vinculando..." else "Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal QR Consentido / URL de Emergencia con selección de campos y renderizado QR local
    if (showQrDialog) {
        var consentName by remember { mutableStateOf(true) }
        var consentPhone by remember { mutableStateOf(true) }
        var consentBlood by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Código QR Consentido") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Selecciona los datos públicos consentidos para el rescatista o médico. La URL es opaca y no incluye datos en texto plano dentro del código QR.",
                        fontSize = 12.sp,
                        color = BodyText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Checkboxes de consentimiento granular
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = consentName, onCheckedChange = { consentName = it })
                        Text("Nombre del asistido", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = consentPhone, onCheckedChange = { consentPhone = it })
                        Text("Teléfono de emergencia", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = consentBlood, onCheckedChange = { consentBlood = it })
                        Text("Tipo de sangre", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            selectedProfile?.let { p ->
                                val fields = mutableListOf<String>()
                                if (consentName) fields.add("presentationName")
                                if (consentPhone) fields.add("emergencyPhone")
                                if (consentBlood) fields.add("bloodType")
                                viewModel.generateQrShare(p.id, fields)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Signal)
                    ) {
                        Text("Generar / Actualizar QR", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    qrShareData?.let { share ->
                        val qrBitmap = remember(share.publicUrl) {
                            QrCodeGenerator.generateQrBitmap(share.publicUrl, 400)
                        }

                        if (qrBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = qrBitmap,
                                contentDescription = "Código QR de Emergencia Escaneable",
                                modifier = Modifier
                                    .size(180.dp)
                                    .padding(8.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )
                        } else {
                            Text("No se pudo generar la imagen del código QR.", color = SosRed, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = share.publicUrl,
                            fontSize = 10.sp,
                            color = Signal,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                selectedProfile?.let { p ->
                                    viewModel.revokeQrShare(p.id, share.shareId)
                                    showQrDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
                        ) {
                            Text("Revocar Acceso QR", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) { Text("Cerrar") }
            }
        )
    }
}
