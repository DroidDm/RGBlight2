package com.rgblight.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import com.rgblight.util.AppLogger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Error(val message: String) : ConnectionState
}

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var connectCont: CancellableContinuation<Boolean>? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val isBleSupported: Boolean get() = adapter != null && adapter.isEnabled

    fun scan(): Flow<List<ScanResult>> = callbackFlow {
        val found = LinkedHashMap<String, ScanResult>()
        val cb = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                if (!found.containsKey(result.device.address))
                    AppLogger.i("BLE", "Found: ${result.device.name ?: "?"} ${result.device.address} rssi=${result.rssi}")
                found[result.device.address] = result
                trySend(found.values.toList())
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { found[it.device.address] = it }
                trySend(found.values.toList())
            }
            override fun onScanFailed(errorCode: Int) {
                AppLogger.e("BLE", "Scan failed: $errorCode")
                close(IllegalStateException("Scan failed: $errorCode"))
            }
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        AppLogger.i("BLE", "Scan started")
        scanner?.startScan(null, settings, cb)
        awaitClose { AppLogger.i("BLE", "Scan stopped"); scanner?.stopScan(cb) }
    }

    suspend fun connect(device: BluetoothDevice): Boolean = suspendCancellableCoroutine { cont ->
        gatt?.let { runCatching { it.disconnect(); it.close() } }
        gatt = null; writeChar = null; _ready.value = false
        connectCont = cont
        _connectionState.value = ConnectionState.Connecting
        AppLogger.i("BLE", "Connecting ${device.name ?: "?"} (${device.address})")
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        cont.invokeOnCancellation {
            runCatching { gatt?.disconnect(); gatt?.close() }
            gatt = null; _connectionState.value = ConnectionState.Disconnected
            _connectedDevice.value = null; _ready.value = false
        }
    }

    fun send(data: ByteArray, useEncryption: Boolean = false): Boolean {
        val g = gatt ?: return false
        val c = writeChar ?: return false
        val payload = if (useEncryption) LotusProtocol.encryptElk(data) else data
        return try {
            AppLogger.bytes("BLE", "TX${if (useEncryption) " enc" else ""}", payload)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(c, payload, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) ==
                        BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                @Suppress("DEPRECATION")
                c.value = payload
                @Suppress("DEPRECATION")
                g.writeCharacteristic(c)
            }
        } catch (t: Throwable) { AppLogger.e("BLE", "send ex", t); false }
    }

    fun disconnect() {
        AppLogger.i("BLE", "Manual disconnect")
        runCatching { gatt?.disconnect(); gatt?.close() }
        gatt = null; writeChar = null
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
        _ready.value = false
    }

    private fun isWritable(c: BluetoothGattCharacteristic): Boolean {
        val p = c.properties
        return (p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
    }

    private fun findInService(s: BluetoothGattService): BluetoothGattCharacteristic? {
        s.getCharacteristic(LotusProtocol.WRITE_UUID)?.takeIf { isWritable(it) }?.let { return it }
        return s.characteristics.firstOrNull { isWritable(it) }
    }

    private fun findWrite(g: BluetoothGatt): BluetoothGattCharacteristic? {
        listOf(
            LotusProtocol.SERVICE_UUID,
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
        ).forEach { uuid ->
            g.getService(uuid)?.let { svc ->
                findInService(svc)?.let { AppLogger.i("BLE", "Write char $uuid: ${it.uuid}"); return it }
            }
        }
        for (svc in g.services) findInService(svc)?.let { AppLogger.i("BLE", "Write char (fb) ${svc.uuid}"); return it }
        return null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            AppLogger.i("BLE", "onConn status=$status new=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.Connected
                _connectedDevice.value = g.device
                runCatching { g.discoverServices() }
                connectCont?.resume(true); connectCont = null
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.Disconnected
                _connectedDevice.value = null; _ready.value = false
                connectCont?.takeIf { it.isActive }?.resume(false); connectCont = null
                runCatching { g.close() }; gatt = null
            }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            AppLogger.i("BLE", "onSvc status=$status services=${g.services.size}")
            g.services.forEach { s ->
                AppLogger.i("BLE", "  svc ${s.uuid}")
                s.characteristics.forEach { AppLogger.i("BLE", "    chr ${it.uuid} props=${it.properties}") }
            }
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val c = findWrite(g) ?: run { AppLogger.e("BLE", "No write char!"); return }
            writeChar = c; _ready.value = true
            AppLogger.i("BLE", "Ready")
        }
        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            AppLogger.i("BLE", "onWrite ${c.uuid} status=$status")
        }
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            c.value?.let { AppLogger.bytes("BLE", "RX ${c.uuid}", it) }
        }
    }
}