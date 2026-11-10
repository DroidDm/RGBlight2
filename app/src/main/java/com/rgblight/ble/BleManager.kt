package com.rgblight.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
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

    val isBleSupported: Boolean
        get() = adapter != null && adapter.isEnabled

    fun scan(): Flow<List<ScanResult>> = callbackFlow {
        val found = LinkedHashMap<String, ScanResult>()
        val cb = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                found[result.device.address] = result
                trySend(found.values.toList())
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { found[it.device.address] = it }
                trySend(found.values.toList())
            }
            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("Scan failed: " + errorCode))
            }
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(null, settings, cb)
        awaitClose { scanner?.stopScan(cb) }
    }

    suspend fun connect(device: BluetoothDevice): Boolean = suspendCancellableCoroutine { cont ->
        gatt?.let { runCatching { it.disconnect(); it.close() } }
        gatt = null
        writeChar = null
        connectCont = cont
        _connectionState.value = ConnectionState.Connecting
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        cont.invokeOnCancellation {
            runCatching { gatt?.disconnect(); gatt?.close() }
            gatt = null
            _connectionState.value = ConnectionState.Disconnected
            _connectedDevice.value = null
        }
    }

    fun send(data: ByteArray): Boolean {
        val g = gatt ?: return false
        val c = writeChar ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(c, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) ==
                        BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                @Suppress("DEPRECATION")
                c.value = data
                @Suppress("DEPRECATION")
                g.writeCharacteristic(c)
            }
        } catch (t: Throwable) { false }
    }

    fun disconnect() {
        runCatching { gatt?.disconnect(); gatt?.close() }
        gatt = null; writeChar = null
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.Connected
                _connectedDevice.value = g.device
                runCatching { g.discoverServices() }
                connectCont?.resume(true); connectCont = null
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.Disconnected
                _connectedDevice.value = null
                if (connectCont?.isActive == true) { connectCont?.resume(false); connectCont = null }
                runCatching { g.close() }; gatt = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = g.getService(LotusProtocol.SERVICE_UUID) ?: return
            writeChar = service.getCharacteristic(LotusProtocol.WRITE_UUID)

            service.getCharacteristic(LotusProtocol.NOTIFY_UUID)?.let { notify ->
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
    }
}