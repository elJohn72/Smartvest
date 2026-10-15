package com.ajtecnology.smartvest.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.espressif.provisioning.DeviceConnectionEvent
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.ProvisionListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

enum class VestWifiStep { IDLE, SEARCHING, SENDING, READY, FAILED }

/** One cancellable attempt. Credentials remain in memory only until a terminal result. */
class VestBleProvisioner(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var scanCallback: ScanCallback? = null
    private var scanTimeout: Runnable? = null
    private var connectionTimeout: Runnable? = null
    private var registered = false
    private var active = false
    private var provisioning = false
    private var generation = 0L
    private var ssid: String? = null
    private var passphrase: String? = null
    private var currentDevice: com.espressif.provisioning.ESPDevice? = null
    private var onUpdate: ((VestWifiStep, String) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun sendWifi(
        bleName: String, pop: String, networkName: String, networkPassword: String,
        onUpdate: (VestWifiStep, String) -> Unit,
    ) {
        close()
        if (networkName.isBlank()) {
            onUpdate(VestWifiStep.FAILED, "Escribe el nombre de la red Wi-Fi.")
            return
        }
        if (adapter?.isEnabled != true) {
            onUpdate(VestWifiStep.FAILED, "Activa el Bluetooth del teléfono.")
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            onUpdate(VestWifiStep.FAILED, "El teléfono no tiene un escáner Bluetooth disponible.")
            return
        }
        val attempt = generation
        active = true
        this.onUpdate = onUpdate
        // SSIDs and proofs are case-sensitive; do not rewrite them.
        ssid = networkName
        passphrase = networkPassword
        val cleanName = bleName.trim()
        onUpdate(VestWifiStep.SEARCHING, "Buscando $cleanName cerca del teléfono.")
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (!active || attempt != generation || scanCallback !== this) return
                val found = result?.scanRecord?.deviceName ?: result?.device?.name
                if (found == cleanName && result != null) {
                    stopScan()
                    handler.post { if (active && attempt == generation) connect(result.device, pop, attempt) }
                }
            }
            override fun onScanFailed(errorCode: Int) {
                finish(attempt, VestWifiStep.FAILED, "No se pudo buscar el chaleco. Revisa Bluetooth y vuelve a intentar.")
            }
        }
        scanCallback = callback
        scanTimeout = Runnable {
            finish(attempt, VestWifiStep.FAILED, "No aparece el chaleco. Enciéndelo y acerca el teléfono.")
        }.also { handler.postDelayed(it, 15_000) }
        try {
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        } catch (_: SecurityException) {
            finish(attempt, VestWifiStep.FAILED, "Faltan permisos de Bluetooth.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: android.bluetooth.BluetoothDevice, pop: String, attempt: Long) {
        onUpdate?.invoke(VestWifiStep.SENDING, "Configurando la red del chaleco.")
        connectionTimeout = Runnable {
            finish(attempt, VestWifiStep.FAILED, "El chaleco no confirmó la configuración. Puedes volver a intentar.")
        }.also { handler.postDelayed(it, 60_000) }
        try {
            EventBus.getDefault().register(this)
            registered = true
            val esp = ESPProvisionManager.getInstance(appContext)
                .createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1)
            currentDevice = esp
            esp.proofOfPossession = pop
            esp.primaryServiceUuid = ESP_PROV_SERVICE_UUID
            esp.connectBLEDevice(device, ESP_PROV_SERVICE_UUID)
        } catch (_: Exception) {
            finish(attempt, VestWifiStep.FAILED, "No se pudo abrir el Bluetooth del chaleco.")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnection(event: DeviceConnectionEvent) {
        if (!active) return
        val attempt = generation
        when (event.eventType) {
            ESPConstants.EVENT_DEVICE_CONNECTED -> {
                val network = ssid ?: return
                val secret = passphrase ?: return
                val esp = currentDevice ?: return
                if (provisioning) return
                provisioning = true
                esp.provision(network, secret, object : ProvisionListener {
                    override fun createSessionFailed(e: Exception?) = fail("La clave de la etiqueta no coincide con este chaleco.")
                    override fun wifiConfigSent() = Unit
                    override fun wifiConfigFailed(e: Exception?) = fail("El chaleco no recibió la red.")
                    override fun wifiConfigApplied() = Unit
                    override fun wifiConfigApplyFailed(e: Exception?) = fail("El chaleco no pudo entrar a esa red.")
                    override fun provisioningFailedFromDevice(reason: ESPConstants.ProvisionFailureReason?) = fail(
                        if (reason == ESPConstants.ProvisionFailureReason.AUTH_FAILED) "La contraseña del Wi-Fi no fue aceptada."
                        else "El chaleco no entró a la red."
                    )
                    override fun deviceProvisioningSuccess() = finish(
                        attempt, VestWifiStep.READY,
                        "Wi-Fi configurado. Comprueba en Monitoreo si llegan lecturas recientes."
                    )
                    override fun onProvisioningFailed(e: Exception?) = fail("No se completó la conexión del chaleco.")
                    private fun fail(message: String) = finish(attempt, VestWifiStep.FAILED, message)
                })
            }
            ESPConstants.EVENT_DEVICE_CONNECTION_FAILED,
            ESPConstants.EVENT_DEVICE_DISCONNECTED -> finish(attempt, VestWifiStep.FAILED, "Se cortó el Bluetooth antes de terminar.")
        }
    }

    fun close() {
        generation++
        active = false
        cleanup()
        onUpdate = null
    }

    private fun finish(attempt: Long, step: VestWifiStep, message: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { finish(attempt, step, message) }
            return
        }
        if (!active || attempt != generation) return
        active = false
        val callback = onUpdate
        cleanup()
        onUpdate = null
        callback?.invoke(step, message)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        scanTimeout?.let(handler::removeCallbacks)
        scanTimeout = null
        scanCallback?.let {
            try { adapter?.bluetoothLeScanner?.stopScan(it) } catch (_: SecurityException) { }
        }
        scanCallback = null
    }

    private fun cleanup() {
        stopScan()
        connectionTimeout?.let(handler::removeCallbacks)
        connectionTimeout = null
        if (registered) {
            EventBus.getDefault().unregister(this)
            registered = false
        }
        try { currentDevice?.disconnectDevice() } catch (_: Exception) { }
        currentDevice = null
        ssid = null
        passphrase = null
        provisioning = false
    }

    companion object {
        const val ESP_PROV_SERVICE_UUID = "021a9004-0382-4aea-bff4-6b3f1b5adfb4"
    }
}
