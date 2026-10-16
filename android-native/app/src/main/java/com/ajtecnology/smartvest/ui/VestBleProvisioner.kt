package com.ajtecnology.smartvest.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.espressif.provisioning.DeviceConnectionEvent
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.ProvisionListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

enum class VestWifiStep { IDLE, SEARCHING, FOUND, SENDING, READY, FAILED }

private const val VEST_NAME_PREFIX = "PROV_SmartVest_"

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
    private var foundDevice: android.bluetooth.BluetoothDevice? = null
    private var onUpdate: ((VestWifiStep, String) -> Unit)? = null
    private val loggedPackets = LinkedHashSet<String>()

    @SuppressLint("MissingPermission")
    fun lookForVest(onUpdate: (VestWifiStep, String) -> Unit) {
        close()
        if (adapter?.isEnabled != true) {
            onUpdate(VestWifiStep.FAILED, "Activa el Bluetooth del teléfono.")
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            onUpdate(VestWifiStep.FAILED, "Este teléfono no puede buscar el chaleco.")
            return
        }
        if (!locationEnabled()) {
            onUpdate(VestWifiStep.FAILED, "Activa la Ubicación del teléfono. Sin eso no aparece el chaleco.")
            return
        }
        val attempt = generation
        active = true
        loggedPackets.clear()
        this.onUpdate = onUpdate
        onUpdate(VestWifiStep.SEARCHING, "Buscando el chaleco. Acércalo.")
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (!active || attempt != generation || scanCallback !== this || result == null) return
                val seen = result.scanRecord?.deviceName ?: result.device?.name ?: localName(result.scanRecord?.bytes)
                val recordBytes = result.scanRecord?.bytes
                val hex = recordBytes?.joinToString("") { "%02x".format(it.toInt() and 0xff) } ?: ""
                if (loggedPackets.size < 8 && loggedPackets.add(hex)) {
                    Log.i("SmartVestBle", "pkt ${seen ?: "-"} n=${recordBytes?.size ?: 0} $hex")
                }
                if (!isSmartVestAdvertisement(result)) return
                val device = result.device
                handler.post {
                    if (!active || attempt != generation) return@post
                    foundDevice = device
                    stopScan()
                    active = false
                    val callbackUpdate = this@VestBleProvisioner.onUpdate
                    this@VestBleProvisioner.onUpdate = null
                    callbackUpdate?.invoke(VestWifiStep.FOUND, "Encontré el chaleco. Escribe el Wi-Fi.")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                finish(attempt, VestWifiStep.FAILED, "No pude buscar. Activa el Bluetooth e inténtalo otra vez.")
            }
        }
        scanCallback = callback
        scanTimeout = Runnable {
            finish(attempt, VestWifiStep.FAILED, "No aparece el chaleco. Enciéndelo y acércalo.")
        }.also { handler.postDelayed(it, 15_000) }
        try {
            scanner.startScan(
                null,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .build(),
                callback,
            )
        } catch (_: SecurityException) {
            finish(attempt, VestWifiStep.FAILED, "Falta el permiso de Bluetooth.")
        } catch (_: IllegalStateException) {
            finish(attempt, VestWifiStep.FAILED, "Activa el Bluetooth del teléfono.")
        }
    }

    @SuppressLint("MissingPermission")
    fun sendWifi(
        networkName: String,
        networkPassword: String,
        onUpdate: (VestWifiStep, String) -> Unit,
    ) {
        val device = foundDevice
        if (networkName.isBlank()) {
            onUpdate(VestWifiStep.FAILED, "Escribe el nombre del Wi-Fi.")
            return
        }
        if (device == null) {
            onUpdate(VestWifiStep.FAILED, "Primero acerca el chaleco.")
            return
        }
        close()
        foundDevice = device
        if (adapter?.isEnabled != true) {
            onUpdate(VestWifiStep.FAILED, "Activa el Bluetooth del teléfono.")
            return
        }
        val attempt = generation
        active = true
        this.onUpdate = onUpdate
        ssid = networkName
        passphrase = networkPassword
        connect(device, attempt)
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: android.bluetooth.BluetoothDevice, attempt: Long) {
        onUpdate?.invoke(VestWifiStep.SENDING, "Configurando la red del chaleco.")
        connectionTimeout = Runnable {
            finish(attempt, VestWifiStep.FAILED, "El chaleco no confirmó la configuración. Puedes volver a intentar.")
        }.also { handler.postDelayed(it, 60_000) }
        try {
            EventBus.getDefault().register(this)
            registered = true
            val esp = ESPProvisionManager.getInstance(appContext)
                .createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_0)
            currentDevice = esp
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
                    override fun createSessionFailed(e: Exception?) = fail("No pude hablar con el chaleco. Acércalo e inténtalo otra vez.")
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
                        "Listo. El chaleco ya tiene la red."
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
        cleanup(clearFound = true)
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
        val keep = foundDevice
        cleanup(clearFound = true)
        if (step == VestWifiStep.FAILED) foundDevice = keep
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

    private fun isSmartVestAdvertisement(result: ScanResult): Boolean {
        val record = result.scanRecord
        val name = (record?.deviceName ?: result.device?.name ?: localName(record?.bytes))?.takeIf { it.isNotBlank() }
        if (name?.startsWith(VEST_NAME_PREFIX) == true) return true
        if (name != null) return false
        val uuids = record?.serviceUuids.orEmpty()
        if (uuids.any { it.uuid.toString().equals(ESP_PROV_SERVICE_UUID, ignoreCase = true) }) return true
        return containsVestUuid(record?.bytes)
    }

    private fun containsVestUuid(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.size < VEST_UUID_LE.size) return false
        for (start in 0..bytes.size - VEST_UUID_LE.size) {
            var same = true
            for (offset in VEST_UUID_LE.indices) {
                if (bytes[start + offset] != VEST_UUID_LE[offset]) {
                    same = false
                    break
                }
            }
            if (same) return true
        }
        return false
    }

    private fun localName(bytes: ByteArray?): String? {
        if (bytes == null) return null
        var index = 0
        var fallback: String? = null
        while (index < bytes.size) {
            val length = bytes[index].toInt() and 0xff
            if (length == 0 || index + length >= bytes.size) break
            val type = bytes[index + 1].toInt() and 0xff
            if (type == 0x09 || type == 0x08) {
                val value = bytes.copyOfRange(index + 2, index + 1 + length).toString(Charsets.UTF_8)
                if (type == 0x09) return value
                fallback = value
            }
            index += length + 1
        }
        return fallback
    }

    private fun locationEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < 28) return true
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return true
        return manager.isLocationEnabled
    }

    private fun cleanup(clearFound: Boolean) {
        stopScan()
        connectionTimeout?.let(handler::removeCallbacks)
        connectionTimeout = null
        if (registered) {
            EventBus.getDefault().unregister(this)
            registered = false
        }
        try { currentDevice?.disconnectDevice() } catch (_: Exception) { }
        currentDevice = null
        if (clearFound) foundDevice = null
        ssid = null
        passphrase = null
        provisioning = false
    }

    companion object {
        const val ESP_PROV_SERVICE_UUID = "021a9004-0382-4aea-bff4-6b3f1b5adfb4"
        private val VEST_UUID_LE = byteArrayOf(
            0xb4.toByte(), 0xdf.toByte(), 0x5a, 0x1b, 0x3f, 0x6b, 0xf4.toByte(), 0xbf.toByte(),
            0xea.toByte(), 0x4a, 0x82.toByte(), 0x03, 0x04, 0x90.toByte(), 0x1a, 0x02,
        )
    }
}
