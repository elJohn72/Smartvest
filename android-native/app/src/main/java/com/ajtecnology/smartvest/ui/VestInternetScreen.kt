package com.ajtecnology.smartvest.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.copyCameraLuminance
import com.ajtecnology.smartvest.data.VestSetupQr
import com.ajtecnology.smartvest.data.parseVestSetupQr
import com.ajtecnology.smartvest.theme.BodyText
import com.ajtecnology.smartvest.theme.Night
import com.ajtecnology.smartvest.theme.Paper
import com.ajtecnology.smartvest.theme.Signal
import com.ajtecnology.smartvest.theme.SosRed
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun VestInternetDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val provisioner = remember { VestBleProvisioner(context) }
    var rawQr by remember { mutableStateOf("") }
    var setup by remember { mutableStateOf<VestSetupQr?>(null) }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var wifiStep by remember { mutableStateOf(VestWifiStep.IDLE) }
    var busy by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val rejected = stringResource(R.string.vest_qr_rejected)

    DisposableEffect(provisioner) {
        onDispose { provisioner.close() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val next = afterPermission
        afterPermission = null
        if (granted.values.all { it }) {
            next?.invoke()
        } else {
            status = context.getString(R.string.vest_permission_needed)
        }
    }

    fun ensurePermissions(required: List<String>, next: () -> Unit) {
        val needed = required.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            next()
        } else {
            afterPermission = next
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    AlertDialog(
        onDismissRequest = { provisioner.close(); password = ""; onDismiss() },
        title = { Text(stringResource(R.string.vest_internet_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.vest_internet_help), color = BodyText)
                Spacer(Modifier.height(12.dp))
                if (setup == null) {
                    OutlinedButton(
                        onClick = { ensurePermissions(listOf(Manifest.permission.CAMERA)) { showCamera = true } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) { Text(stringResource(R.string.vest_scan_qr)) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawQr,
                        onValueChange = { if (it.length <= 1024) rawQr = it },
                        label = { Text(stringResource(R.string.vest_qr_paste)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val parsed = parseVestSetupQr(rawQr)
                            if (parsed == null) status = rejected else {
                                setup = parsed
                                rawQr = ""
                                status = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Signal),
                    ) { Text(stringResource(R.string.vest_qr_use)) }
                } else {
                    Text(setup!!.name, color = Night)
                    Spacer(Modifier.height(8.dp))
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
                status?.let {
                    val color = when (wifiStep) {
                        VestWifiStep.READY -> Signal
                        VestWifiStep.SEARCHING, VestWifiStep.SENDING -> BodyText
                        else -> SosRed
                    }
                    Text(it, color = color, modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            if (setup != null) {
                Button(
                    onClick = {
                        val label = setup ?: return@Button
                        ensurePermissions(vestBlePermissions()) {
                            busy = true
                            status = null
                            provisioner.sendWifi(label.name, label.pop, ssid, password) { step, message ->
                                busy = step == VestWifiStep.SEARCHING || step == VestWifiStep.SENDING
                                wifiStep = step
                                if (step == VestWifiStep.READY) password = ""
                                status = message
                            }
                        }
                    },
                    enabled = !busy && ssid.isNotBlank() && wifiStep != VestWifiStep.READY,
                    colors = ButtonDefaults.buttonColors(containerColor = Signal),
                ) { Text(stringResource(R.string.vest_wifi_send)) }
            }
        },
        dismissButton = {
            TextButton(onClick = { provisioner.close(); password = ""; onDismiss() }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )

    if (showCamera) {
        VestQrCamera(
            onResult = { text ->
                showCamera = false
                val parsed = parseVestSetupQr(text)
                if (parsed == null) status = rejected else {
                    setup = parsed
                    rawQr = ""
                    status = null
                }
            },
            onDismiss = { showCamera = false },
        )
    }
}

@Composable
private fun VestQrCamera(onResult: (String) -> Unit, onDismiss: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    val preview = remember { Preview.Builder().build() }
    val analysis = remember {
        ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
    }
    val latestResult by rememberUpdatedState(onResult)
    var cameraError by remember { mutableStateOf(false) }
    val disposed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    DisposableEffect(lifecycleOwner, analysis) {
        onDispose {
            disposed.set(true)
            analysis.clearAnalyzer()
            if (providerFuture.isDone) {
                try { providerFuture.get().unbind(preview, analysis) } catch (_: Exception) { }
            }
            executor.shutdownNow()
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Night)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    providerFuture.addListener({
                        if (disposed.get()) return@addListener
                        try {
                            val provider = providerFuture.get()
                            preview.surfaceProvider = previewView.surfaceProvider
                            val reader = MultiFormatReader().apply {
                                setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                            }
                            var done = false
                            analysis.setAnalyzer(executor) { image ->
                                try {
                                    if (done || disposed.get()) return@setAnalyzer
                                    val plane = image.planes[0]
                                    val data = copyCameraLuminance(plane.buffer, image.width, image.height, plane.rowStride, plane.pixelStride)
                                        ?: return@setAnalyzer
                                    val source = PlanarYUVLuminanceSource(data, image.width, image.height, 0, 0, image.width, image.height, false)
                                    val text = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
                                    done = true
                                    previewView.post { if (!disposed.get()) latestResult(text) }
                                } catch (_: com.google.zxing.ReaderException) {
                                    // A frame without a QR is expected; never log the QR/PoP.
                                } finally {
                                    reader.reset()
                                    image.close()
                                }
                            }
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        } catch (_: Exception) {
                            cameraError = true
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )
            Column(Modifier.padding(24.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cancel), color = Paper)
                }
                if (cameraError) Text("No se pudo abrir la cámara. Puedes cerrar y pegar el contenido de la etiqueta.", color = Paper)
            }
        }
    }
}

private fun vestBlePermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= 31) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
        )
    }
}
