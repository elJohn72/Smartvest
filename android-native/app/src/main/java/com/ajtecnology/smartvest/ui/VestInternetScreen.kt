package com.ajtecnology.smartvest.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.theme.BodyText
import com.ajtecnology.smartvest.theme.Signal
import com.ajtecnology.smartvest.theme.SosRed

@Composable
fun VestInternetDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val provisioner = remember { VestBleProvisioner(context) }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Buscando el chaleco. Acércalo.") }
    var step by remember { mutableStateOf(VestWifiStep.SEARCHING) }
    var vestFound by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val permissionText = stringResource(R.string.vest_permission_needed)

    DisposableEffect(provisioner) {
        onDispose { provisioner.close() }
    }

    fun search() {
        busy = true
        vestFound = false
        password = ""
        provisioner.lookForVest { next, message ->
            step = next
            status = message
            if (next == VestWifiStep.FOUND) vestFound = true
            busy = next == VestWifiStep.SEARCHING
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) search() else {
            step = VestWifiStep.FAILED
            status = permissionText
            busy = false
        }
    }

    fun ensureBluetooth(next: () -> Unit) {
        val needed = vestBlePermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) next() else permissionLauncher.launch(needed.toTypedArray())
    }

    LaunchedEffect(Unit) {
        ensureBluetooth { search() }
    }

    AlertDialog(
        onDismissRequest = { provisioner.close(); password = ""; onDismiss() },
        title = { Text(stringResource(R.string.vest_internet_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.vest_internet_help), color = BodyText)
                Spacer(Modifier.height(12.dp))
                Text(
                    status,
                    color = if (step == VestWifiStep.READY) Signal else if (step == VestWifiStep.FAILED) SosRed else BodyText,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (vestFound && step != VestWifiStep.READY) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        label = { Text(stringResource(R.string.vest_wifi_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.vest_wifi_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                if (step == VestWifiStep.FAILED) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { ensureBluetooth { search() } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) { Text(stringResource(R.string.vest_search_again)) }
                }
            }
        },
        confirmButton = {
            if (vestFound && step != VestWifiStep.READY && step != VestWifiStep.SEARCHING) {
                Button(
                    onClick = {
                        busy = true
                        provisioner.sendWifi(ssid, password) { next, message ->
                            step = next
                            status = message
                            busy = next == VestWifiStep.SENDING
                            if (next == VestWifiStep.READY) password = ""
                        }
                    },
                    enabled = !busy && ssid.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Signal),
                ) { Text(stringResource(R.string.vest_wifi_send)) }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { provisioner.close(); password = ""; onDismiss() },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun vestBlePermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= 31) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
        )
    }
}
