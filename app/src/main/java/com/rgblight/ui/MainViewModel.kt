package com.rgblight.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rgblight.App
import com.rgblight.ble.LotusProtocol
import com.rgblight.data.*
import com.rgblight.service.LedService
import com.rgblight.util.AppLogger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

data class ColorUiState(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val value: Float = 100f
)

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ble = (app as App).bleManager
    private val repo = (app as App).settings
    private val savedRepo = (app as App).saved

    val connectionState = ble.connectionState
    val connectedDevice = ble.connectedDevice
    val settings = repo.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, AppSettings()
    )

    val savedColors = savedRepo.savedColors.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val savedEffects = savedRepo.savedEffects.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val savedMixes = savedRepo.savedMixes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _devices = MutableStateFlow<List<ScanResult>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _colorState = MutableStateFlow(ColorUiState())
    val colorState = _colorState.asStateFlow()

    private val _powerOn = MutableStateFlow(true)
    val powerOn = _powerOn.asStateFlow()

    private val _effectSpeed = MutableStateFlow(128)
    val effectSpeed = _effectSpeed.asStateFlow()
    private val _effectBrightness = MutableStateFlow(100)
    val effectBrightness = _effectBrightness.asStateFlow()
    private val _currentEffectId = MutableStateFlow<Int?>(null)
    val currentEffectId = _currentEffectId.asStateFlow()

    private val _mixColors = MutableStateFlow<List<Int>>(emptyList())
    val mixColors = _mixColors.asStateFlow()
    private val _mixMode = MutableStateFlow(MixMode.FADE)
    val mixMode = _mixMode.asStateFlow()
    private val _mixSpeed = MutableStateFlow(128)
    val mixSpeed = _mixSpeed.asStateFlow()
    private val _mixBrightness = MutableStateFlow(100)
    val mixBrightness = _mixBrightness.asStateFlow()
    private val _isMixPlaying = MutableStateFlow(false)
    val isMixPlaying = _isMixPlaying.asStateFlow()

    private var scanJob: Job? = null

    private sealed interface PendingCmd {
        data class Rgb(val r: Int, val g: Int, val b: Int) : PendingCmd
        data class Bright(val v: Int) : PendingCmd
    }

    private val pending = MutableSharedFlow<PendingCmd>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val persistFlow = MutableSharedFlow<LastColor>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        AppLogger.i("UI", "ViewModel created")
        viewModelScope.launch {
            settings.collectLatest { s ->
                if (_colorState.value == ColorUiState() && s.lastColor != LastColor()) {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(
                        android.graphics.Color.rgb(s.lastColor.r, s.lastColor.g, s.lastColor.b), hsv
                    )
                    _colorState.value = ColorUiState(hsv[0], hsv[1], s.lastColor.brightness.toFloat())
                }
            }
        }
        viewModelScope.launch {
            pending.sample(60L).collect { cmd ->
                when (cmd) {
                    is PendingCmd.Rgb -> {
                        AppLogger.i("UI", "Send color R=${cmd.r} G=${cmd.g} B=${cmd.b}")
                        ble.send(LotusProtocol.color(cmd.r, cmd.g, cmd.b))
                    }
                    is PendingCmd.Bright -> {
                        AppLogger.i("UI", "Send brightness ${cmd.v}")
                        ble.send(LotusProtocol.brightness(cmd.v))
                    }
                }
            }
        }
        viewModelScope.launch {
            persistFlow.debounce(500L).collect { repo.setLastColor(it) }
        }
        viewModelScope.launch {
            savedRepo.activeMix.collect { s ->
                if (s != null && _mixColors.value.isEmpty() && s.colors.isNotEmpty()) {
                    _mixColors.value = s.colors
                    _mixMode.value = s.mode
                    _mixSpeed.value = s.speed
                    _mixBrightness.value = s.brightness
                }
            }
        }
        viewModelScope.launch {
            val s = settings.first()
            if (s.autoConnect && s.lastDeviceMac.isNotBlank() && ble.isBleSupported) {
                AppLogger.i("UI", "Auto-connect to ${s.lastDeviceName} (${s.lastDeviceMac})")
                runCatching {
                    val adapter = (getApplication<App>())
                        .getSystemService(android.bluetooth.BluetoothManager::class.java).adapter
                    val dev = adapter.getRemoteDevice(s.lastDeviceMac)
                    connect(dev)
                }.onFailure { AppLogger.e("UI", "Auto-connect failed", it) }
            }
        }
    }

    fun startScan() {
        AppLogger.i("UI", "startScan")
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            ble.scan().collect { _devices.value = it }
        }
    }
    fun stopScan() { scanJob?.cancel() }

    fun connect(device: BluetoothDevice) = viewModelScope.launch {
        AppLogger.i("UI", "connect(${device.name ?: "?"} / ${device.address})")
        val ok = ble.connect(device)
        AppLogger.i("UI", "connect result: $ok")
        if (ok) {
            repo.setLastDevice(device.address, device.name ?: "RGB light")

            // Ждём, пока найдётся writable characteristic (до 3 секунд)
            val becameReady = withTimeoutOrNull(3000) {
                ble.ready.first { it }
            } ?: false
            AppLogger.i("UI", "BLE ready: $becameReady")

            val s = settings.value
            if (becameReady && s.timeSync) {
                AppLogger.i("UI", "Sending timeSync")
                delay(150)
                ble.send(LotusProtocol.timeSync())
                delay(80)
            }
            if (becameReady) {
                val lc = s.lastColor
                AppLogger.i("UI", "Applying saved color R=${lc.r} G=${lc.g} B=${lc.b} bright=${lc.brightness}")
                ble.send(LotusProtocol.color(lc.r, lc.g, lc.b))
                delay(80)
                ble.send(LotusProtocol.brightness(lc.brightness))
            } else {
                AppLogger.w("UI", "BLE not ready after 3s — skip initial commands")
            }

            LedService.start(getApplication(), device.name ?: "RGB light")
        }
    }

    fun disconnect() {
        AppLogger.i("UI", "disconnect()")
        ble.disconnect()
        LedService.stop(getApplication())
    }

    fun togglePower() = viewModelScope.launch {
        _powerOn.value = !_powerOn.value
        AppLogger.i("UI", "Power ${if (_powerOn.value) "ON" else "OFF"}")
        ble.send(if (_powerOn.value) LotusProtocol.powerOn() else LotusProtocol.powerOff())
    }

    fun updateColor(hue: Float, sat: Float) {
        _colorState.value = _colorState.value.copy(hue = hue, saturation = sat)
        val rgb = hsvToRgb(hue, sat, 1f)
        pending.tryEmit(PendingCmd.Rgb(rgb[0], rgb[1], rgb[2]))
        schedulePersist(rgb)
    }

    fun updateBrightness(value: Float) {
        _colorState.value = _colorState.value.copy(value = value)
        val level = (value.coerceIn(0f, 100f) * 2.55f).toInt().coerceIn(0, 255)
        pending.tryEmit(PendingCmd.Bright(level))
        val rgb = hsvToRgb(_colorState.value.hue, _colorState.value.saturation, 1f)
        schedulePersist(rgb, value.toInt())
    }

    private fun schedulePersist(rgb: IntArray, brightness: Int = _colorState.value.value.toInt()) {
        persistFlow.tryEmit(LastColor(rgb[0], rgb[1], rgb[2], brightness))
    }

    private fun hsvToRgb(hue: Float, sat: Float, v: Float): IntArray {
        val int = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, v))
        return intArrayOf(int shr 16 and 0xFF, int shr 8 and 0xFF, int and 0xFF)
    }

    fun startEffect(effectId: Int) = viewModelScope.launch {
        AppLogger.i("UI", "startEffect #$effectId")
        _currentEffectId.value = effectId
        ble.send(LotusProtocol.brightness((_effectBrightness.value * 2.55f).toInt().coerceIn(0, 255)))
        ble.send(LotusProtocol.effect(effectId, _effectSpeed.value))
    }

    fun setEffectSpeed(v: Int) {
        _effectSpeed.value = v.coerceIn(0, 255)
        _currentEffectId.value?.let { id ->
            viewModelScope.launch { ble.send(LotusProtocol.effect(id, _effectSpeed.value)) }
        }
    }

    fun setEffectBrightness(v: Int) {
        _effectBrightness.value = v.coerceIn(0, 100)
        viewModelScope.launch {
            ble.send(LotusProtocol.brightness((_effectBrightness.value * 2.55f).toInt().coerceIn(0, 255)))
        }
    }

    fun stopEffect() = viewModelScope.launch {
        AppLogger.i("UI", "stopEffect")
        _currentEffectId.value = null
        val rgb = hsvToRgb(_colorState.value.hue, _colorState.value.saturation, 1f)
        ble.send(LotusProtocol.color(rgb[0], rgb[1], rgb[2]))
    }

    fun addMixColor(rgb: Int) {
        if (_mixColors.value.size >= 12) return
        _mixColors.value = _mixColors.value + (rgb and 0xFFFFFF)
        AppLogger.i("UI", "addMixColor #${_mixColors.value.size}")
        persistMix()
    }

    fun removeMixColor(index: Int) {
        _mixColors.value = _mixColors.value.toMutableList().also { it.removeAt(index) }
        persistMix()
    }

    fun clearMix() {
        _mixColors.value = emptyList()
        persistMix()
    }

    fun moveMixColor(from: Int, to: Int) {
        val list = _mixColors.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _mixColors.value = list
        persistMix()
    }

    fun setMixMode(m: MixMode) { _mixMode.value = m; persistMix() }
    fun setMixSpeed(v: Int) { _mixSpeed.value = v.coerceIn(0, 255); persistMix() }
    fun setMixBrightness(v: Int) { _mixBrightness.value = v.coerceIn(0, 100); persistMix() }

    fun startMix() = viewModelScope.launch {
        if (_mixColors.value.isEmpty()) return@launch
        AppLogger.i("UI", "startMix colors=${_mixColors.value.size} mode=${_mixMode.value} speed=${_mixSpeed.value}")
        _isMixPlaying.value = true
        ble.send(LotusProtocol.brightness((_mixBrightness.value * 2.55f).toInt().coerceIn(0, 255)))
        LedService.playMix(
            getApplication(),
            _mixColors.value,
            _mixMode.value,
            _mixSpeed.value,
            _mixBrightness.value
        )
    }

    fun stopMix() {
        AppLogger.i("UI", "stopMix")
        _isMixPlaying.value = false
        LedService.stopMix(getApplication())
        viewModelScope.launch {
            val rgb = hsvToRgb(_colorState.value.hue, _colorState.value.saturation, 1f)
            ble.send(LotusProtocol.color(rgb[0], rgb[1], rgb[2]))
        }
    }

    private fun persistMix() {
        viewModelScope.launch {
            savedRepo.setActiveMix(
                ActiveMixState(
                    colors = _mixColors.value,
                    mode = _mixMode.value,
                    speed = _mixSpeed.value,
                    brightness = _mixBrightness.value
                )
            )
        }
    }

    fun saveCurrentColor(name: String) = viewModelScope.launch {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(
            _colorState.value.hue, _colorState.value.saturation, 1f
        ))
        val r = android.graphics.Color.red(rgb)
        val g = android.graphics.Color.green(rgb)
        val b = android.graphics.Color.blue(rgb)
        val hex = "#%02X%02X%02X".format(r, g, b)
        savedRepo.saveColor(
            SavedColor(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { hex },
                r = r, g = g, b = b
            )
        )
    }

    fun saveEffectWithCurrentState(effectId: Int, name: String) = viewModelScope.launch {
        savedRepo.saveEffect(
            SavedEffect(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { EffectCatalog.byId(effectId)?.name ?: "Эффект" },
                effectId = effectId,
                speed = _effectSpeed.value,
                brightness = _effectBrightness.value
            )
        )
    }

    fun saveCurrentMix(name: String) = viewModelScope.launch {
        if (_mixColors.value.isEmpty()) return@launch
        savedRepo.saveMix(
            SavedMix(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Микс ${savedMixes.value.size + 1}" },
                colors = _mixColors.value,
                mode = _mixMode.value,
                speed = _mixSpeed.value,
                brightness = _mixBrightness.value
            )
        )
    }

    fun applySavedColor(c: SavedColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.rgb(c.r, c.g, c.b), hsv)
        _colorState.value = ColorUiState(hsv[0], hsv[1], _colorState.value.value)
        viewModelScope.launch { ble.send(LotusProtocol.color(c.r, c.g, c.b)) }
    }

    fun applySavedEffect(e: SavedEffect) {
        _effectSpeed.value = e.speed
        _effectBrightness.value = e.brightness
        _currentEffectId.value = e.effectId
        viewModelScope.launch {
            ble.send(LotusProtocol.brightness((e.brightness * 2.55f).toInt().coerceIn(0, 255)))
            ble.send(LotusProtocol.effect(e.effectId, e.speed))
        }
    }

    fun applySavedMix(m: SavedMix) {
        _mixColors.value = m.colors
        _mixMode.value = m.mode
        _mixSpeed.value = m.speed
        _mixBrightness.value = m.brightness
        startMix()
    }

    fun deleteSavedColor(id: String) = viewModelScope.launch { savedRepo.deleteColor(id) }
    fun deleteSavedEffect(id: String) = viewModelScope.launch { savedRepo.deleteEffect(id) }
    fun deleteSavedMix(id: String) = viewModelScope.launch { savedRepo.deleteMix(id) }

    fun setTheme(v: ThemeMode) = viewModelScope.launch { repo.setTheme(v) }
    fun setAutoConnect(v: Boolean) = viewModelScope.launch { repo.setAutoConnect(v) }
    fun setElkEncryption(v: Boolean) = viewModelScope.launch { repo.setElkEncryption(v) }
    fun setTimeSync(v: Boolean) = viewModelScope.launch { repo.setTimeSync(v) }
    fun setMixBackground(v: MixBackgroundMode) = viewModelScope.launch { repo.setMixBackground(v) }
    fun setThrottle(v: Int) = viewModelScope.launch { repo.setThrottle(v) }
    fun setUpdateUrl(v: String) = viewModelScope.launch { repo.setUpdateUrl(v) }

    override fun onCleared() {
        AppLogger.i("UI", "ViewModel cleared")
        scanJob?.cancel()
        super.onCleared()
    }
}