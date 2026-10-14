package com.ajtecnology.smartvest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.ProfileDto
import com.ajtecnology.smartvest.data.ShareCreateResponse
import com.ajtecnology.smartvest.theme.BodyText
import com.ajtecnology.smartvest.theme.Signal
import com.ajtecnology.smartvest.theme.SosRed

@Composable
fun ProfileFormDialog(
    title: String,
    initial: ProfileDto?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, blood: String, notes: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.presentationName.orEmpty()) }
    var phone by remember(initial?.id) { mutableStateOf(initial?.emergencyPhone.orEmpty()) }
    var blood by remember(initial?.id) { mutableStateOf(initial?.bloodType.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.medicalObservations.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val nameRequired = stringResource(R.string.profile_name_required)
    val saveError = stringResource(R.string.profile_save_error)
    val scrollMax = (LocalConfiguration.current.screenHeightDp * 0.42f).dp

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = scrollMax).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.profile_private_note), color = BodyText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.profile_phone_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = blood,
                    onValueChange = { blood = it },
                    label = { Text(stringResource(R.string.profile_blood_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.profile_notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = SosRed, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = nameRequired
                        return@Button
                    }
                    saving = true
                    error = null
                    onSave(name.trim(), phone.trim(), blood.trim(), notes.trim()) { ok ->
                        saving = false
                        if (ok) onDismiss() else error = saveError
                    }
                },
                enabled = !saving,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (saving) stringResource(R.string.status_loading) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !saving,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun PairDeviceDialog(
    onDismiss: () -> Unit,
    onPair: (deviceId: String, code: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var deviceId by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pairing by remember { mutableStateOf(false) }
    val pairError = stringResource(R.string.pair_error)
    val scrollMax = (LocalConfiguration.current.screenHeightDp * 0.42f).dp

    AlertDialog(
        onDismissRequest = { if (!pairing) onDismiss() },
        title = { Text(stringResource(R.string.pair_title)) },
        text = {
            Column(modifier = Modifier.heightIn(max = scrollMax).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.pair_help), color = BodyText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text(stringResource(R.string.device_id_label)) },
                    placeholder = { Text("VEST-001") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.pairing_code_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error != null) {
                    Text(error!!, color = SosRed, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    pairing = true
                    error = null
                    onPair(deviceId.trim(), code.trim()) { ok ->
                        pairing = false
                        if (ok) onDismiss() else error = pairError
                    }
                },
                enabled = !pairing && deviceId.isNotBlank() && code.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (pairing) stringResource(R.string.pairing_progress) else stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !pairing,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun QrShareDialog(
    share: ShareCreateResponse?,
    onDismiss: () -> Unit,
    onGenerate: (fields: List<String>, onDone: (Boolean) -> Unit) -> Unit,
    onRevoke: (shareId: String, onDone: (Boolean) -> Unit) -> Unit
) {
    var consentName by remember { mutableStateOf(true) }
    var consentPhone by remember { mutableStateOf(false) }
    var consentBlood by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var revoked by remember { mutableStateOf(false) }
    val needField = stringResource(R.string.qr_need_field)
    val qrError = stringResource(R.string.qr_error)
    val scrollMax = (LocalConfiguration.current.screenHeightDp * 0.46f).dp

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(stringResource(R.string.qr_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = scrollMax)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.qr_help), color = BodyText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ConsentRow(stringResource(R.string.qr_consent_name), consentName) { consentName = it }
                ConsentRow(stringResource(R.string.qr_consent_phone), consentPhone) { consentPhone = it }
                ConsentRow(stringResource(R.string.qr_consent_blood), consentBlood) { consentBlood = it }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val fields = buildList {
                            if (consentName) add("presentationName")
                            if (consentPhone) add("emergencyPhone")
                            if (consentBlood) add("bloodType")
                        }
                        if (fields.isEmpty()) {
                            error = needField
                            return@Button
                        }
                        working = true
                        error = null
                        revoked = false
                        onGenerate(fields) { ok ->
                            working = false
                            if (!ok) error = qrError
                        }
                    },
                    enabled = !working,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Signal)
                ) {
                    Text(if (working) stringResource(R.string.qr_generating) else stringResource(R.string.generate_qr_button))
                }
                if (error != null) {
                    Text(error!!, color = SosRed, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (revoked) {
                    Text(
                        stringResource(R.string.qr_revoked),
                        color = BodyText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                share?.let { current ->
                    val qrBitmap = remember(current.publicUrl) {
                        QrCodeGenerator.generateQrBitmap(current.publicUrl, 400)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap,
                            contentDescription = stringResource(R.string.qr_image_description),
                            modifier = Modifier
                                .size(180.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    } else {
                        Text(qrError, color = SosRed, fontSize = 14.sp)
                    }
                    Text(current.publicUrl, color = Signal, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            working = true
                            onRevoke(current.shareId) { ok ->
                                working = false
                                if (ok) revoked = true else error = qrError
                            }
                        },
                        enabled = !working,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SosRed)
                    ) {
                        Text(stringResource(R.string.revoke_qr_button))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !working,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun ConsentRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ConfirmDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: (onDone: (Boolean) -> Unit) -> Unit
) {
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    val deleteError = stringResource(R.string.delete_profile_error)

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(stringResource(R.string.delete_profile_title)) },
        text = {
            Column {
                Text(stringResource(R.string.delete_profile_body), color = BodyText, fontSize = 16.sp)
                if (error != null) {
                    Text(error!!, color = SosRed, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    working = true
                    error = null
                    onConfirm { ok ->
                        working = false
                        if (ok) onDismiss() else error = deleteError
                    }
                },
                enabled = !working,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SosRed)
            ) { Text(stringResource(R.string.remove_profile)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !working,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.cancel)) }
        }
    )
}
