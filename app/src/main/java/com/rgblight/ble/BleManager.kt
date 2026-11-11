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

    val isBleSupported: Boolean
        get() = adapter != null && adapter.isEnabled

    fun scan(): Flow<List<ScanResult>> = callbackFlow {
        val found = LinkedHashMap<String, ScanResult>()
        val cb = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                if (!found.containsKey(result.device.address)) {
                    AppLogger.i("BLE", "Found: ${result.device.name ?: "?"} ${result.device.address} rssi=${result.rssi}")
                }
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
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        AppLogger.i("BLE", "Scan started")
        scanner?.startScan(null, settings, cb)
        awaitClose {
            AppLogger.i("BLE", "Scan stopped")
            scanner?.stopScan(cb)
        }
    }

    suspend fun connect(device: BluetoothDevice): Boolean = suspendCancellableCoroutine { cont ->
        gatt?.let { runCatching { it.disconnect(); it.close() } }
        gatt = null
        writeChar = null
        _ready.value = false
        connectCont = cont
        _connectionState.value = ConnectionState.Connecting
        AppLogger.i("BLE", "Connecting to ${device.name ?: "?"} (${device.address})")
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        cont.invokeOnCancellation {
            runCatching { gatt?.disconnect(); gatt?.close() }
            gatt = null
            _connectionState.value = ConnectionState.Disconnected
            _connectedDevice.value = null
            _ready.value = false
            AppLogger.w("BLE", "Connection cancelled")
        }
    }

    fun send(data: ByteArray): Boolean {
        val g = gatt
        val c = writeChar
        if (g == null || c == null) {
            AppLogger.w("BLE", "send() skipped — not ready (gatt=${g != null}, char=${c != null})")
            return false
        }
        return try {
            val writeType = chooseWriteType(c)
            AppLogger.logBytes("BLE", "TX → ${c.uuid}", data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = g.writeCharacteristic(c, data, writeType)
                if (result != BluetoothStatusCodes.SUCCESS) {
                    AppLogger.w("BLE", "writeCharacteristic result=$result")
                }
                result == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                c.writeType = writeType
                @Suppress("DEPRECATION")
                c.value = data
                @Suppress("DEPRECATION")
                g.writeCharacteristic(c)
            }
        } catch (t: Throwable) {
            AppLogger.e("BLE", "send() exception", t)
            false
        }
    }

    fun disconnect() {
        AppLogger.i("BLE", "Manual disconnect")
        runCatching { gatt?.disconnect(); gatt?.close() }
        gatt = null
        writeChar = null
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
        _ready.value = false
    }

    private fun chooseWriteType(c: BluetoothGattCharacteristic): Int {
        val p = c.properties
        return if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
    }

    private fun isWritable(c: BluetoothGattCharacteristic): Boolean {
        val p = c.properties
        return (p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
    }

    private fun findInService(service: BluetoothGattService): BluetoothGattCharacteristic? {
        val known = service.getCharacteristic(LotusProtocol.WRITE_UUID)
        if (known != null && isWritable(known)) return known
        return service.characteristics.firstOrNull { isWritable(it) }
    }

    private fun findWriteCharacteristic(g: BluetoothGatt): BluetoothGattCharacteristic? {
        val preferredServices = listOf(
            LotusProtocol.SERVICE_UUID,
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
        )
        for (uuid in preferredServices) {
            val service = g.getService(uuid) ?: continue
            val char = findInService(service)
            if (char != null) {
                AppLogger.i("BLE", "Write char in preferred service $uuid: ${char.uuid}")
                return char
            }
        }
        for (service in g.services) {
            val char = findInService(service)
            if (char != null) {
                AppLogger.i("BLE", "Write char (fallback) in ${service.uuid}: ${char.uuid}")
                return char
            }
        }
        return null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            AppLogger.i("BLE", "onConnectionStateChange status=$status newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.Connected
                _connectedDevice.value = g.device
                runCatching { g.discoverServices() }
                connectCont?.resume(true); connectCont = null
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.Disconnected
                _connectedDevice.value = null
                _ready.value = false
                if (connectCont?.isActive == true) { connectCont?.resume(false); connectCont = null }
                runCatching { g.close() }; gatt = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            AppLogger.i("BLE", "onServicesDiscovered status=$status, ${g.services.size} services")
            g.services.forEach { s ->
                AppLogger.i("BLE", "  svc ${s.uuid}")
                s.characteristics.forEach { c ->
                    AppLogger.i("BLE", "    chr ${c.uuid} props=${c.properties}")
                }
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                AppLogger.e("BLE", "onServicesDiscovered failed status=$status")
                return
            }

            val char = findWriteCharacteristic(g)
            if (char == null) {
                AppLogger.e("BLE", "No writable characteristic found!")
                _ready.value = false
                return
            }
            writeChar = char
            _ready.value = true
            AppLogger.i("BLE", "Ready. writeType=${chooseWriteType(char)}")

            g.getService(LotusProtocol.SERVICE_UUID)
                ?.getCharacteristic(LotusProtocol.NOTIFY_UUID)
                ?.let { notify ->
                    runCatching { g.setCharacteristicNotification(notify, true) }
                    notify.getDescriptor(LotusProtocol.CCCD_UUID)?.let { desc ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            g.writeDescriptor(desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            g.writeDescriptor(desc)
                        }
                    }
                }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            AppLogger.i("BLE", "onCharacteristicWrite uuid=${c.uuid} status=$status")
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val v = c.value ?: return
            AppLogger.logBytes("BLE", "RX ← ${c.uuid}", v)
        }
    }
}