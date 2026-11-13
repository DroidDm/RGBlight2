package com.rgblight.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rgblight.App
import com.rgblight.ble.ConnectionState
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

/** Цвета — полные ARGB (0xFFRRGGBB), иначе Color(Int) даст прозрачный. */
data class MixUiStep(
    val kind: StepKind,
    val color: Int = 0xFFFFFFFF.toInt(),
    val effectMode: EffectMode = EffectMode.FADE,
    val colors: List<Int> = listOf(
        0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt()
    ),
    val holdMs: Int = 1000,
    val rampMs: Int = 500
) {
    fun toModel(): MixStep = MixStep(kind, color, effectMode, colors, holdMs, rampMs)
}

enum class MusicSource { CONTROLLER, PHONE }

private val DEFAULT_EFFECT_COLORS: List<Int> = listOf(
    0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt()
)

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ble = (app as App).bleManager
    private val repo = (app as App).settings
    private val savedRepo = (app as App).saved

    val connectionState = ble.connectionState
    val connectedDevice = ble.connectedDevice
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val savedColors = savedRepo.savedColors.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val savedEffects = savedRepo.savedEffects.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val savedMixes = savedRepo.savedMixes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Стартуем с 10 пустыми слотами, чтобы в UI сразу была кнопка «+».
    val colorSlots = savedRepo.colorSlots.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        (0 until 10).map { ColorSlot(it, null) }
    )

    // ===== Скан =====
    private val _devices = MutableStateFlow<List<ScanResult>>(emptyList())
    val devices = _devices.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()
    private var scanJob: Job? = null

    // ===== Цвет =====
    private val _colorState = MutableStateFlow(ColorUiState())
    val colorState = _colorState.asStateFlow()

    private val _activeSlot = MutableStateFlow<Int?>(null)
    val activeSlot = _activeSlot.asStateFlow()

    private val _powerOn = MutableStateFlow(true)
    val powerOn = _powerOn.asStateFlow()

    // ===== Эффект (общий) =====
    private val _effectMode = MutableStateFlow(EffectMode.FADE)
    val effectMode = _effectMode.asStateFlow()
    private val _effectColors = MutableStateFlow(DEFAULT_EFFECT_COLORS)
    val effectColors = _effectColors.asStateFlow()
    private val _protocolId = MutableStateFlow<Int?>(null)
    val protocolId = _protocolId.asStateFlow()
    private val _isEffectPlaying = MutableStateFlow(false)
    val isEffectPlaying = _isEffectPlaying.asStateFlow()

    // ===== Микс =====
    private val _mixSteps = MutableStateFlow<List<MixUiStep>>(emptyList())
    val mixSteps = _mixSteps.asStateFlow()
    private val _isMixPlaying = MutableStateFlow(false)
    val isMixPlaying = _isMixPlaying.asStateFlow()

    // ===== Музыка =====
    private val _musicSource = MutableStateFlow(MusicSource.CONTROLLER)
    val musicSource = _musicSource.asStateFlow()
    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying = _isMusicPlaying.asStateFlow()

    // ===== Сон-таймер =====
    private val _sleepUntilMs = MutableStateFlow<Long?>(null)
    val sleepUntilMs = _sleepUntilMs.asStateFlow()

    // ===== Отправка цвета/яркости с throttle =====
    private sealed interface Pending {
        data class Rgb(val r: Int, val g: Int, val b: Int) : Pending
        data class Bright(val v: Int) : Pending
    }
    private val pending = MutableSharedFlow<Pending>(
        extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val persistFlow = MutableSharedFlow<LastColor>(
        extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val slotPersist = MutableSharedFlow<Int>(
        extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            settings.collectLatest { s ->
                if (_colorState.value.value == 100f && s.lastColor != LastColor()) {
                    val hsv = FloatArray(3)
                    Color.colorToHSV(Color.rgb(s.lastColor.r, s.lastColor.g, s.lastColor.b), hsv)
                    _colorState.value = ColorUiState(hsv[0], hsv[1], s.lastColor.brightness.toFloat())
                }
            }
        }
        viewModelScope.launch {
            settings.flatMapLatest { it.throttleMs.let { t -> flowOf(t) } }
                .collectLatest { }
        }
        viewModelScope.launch {
            pending.sample(60L).collect { cmd ->
                when (cmd) {
                    is Pending.Rgb -> ble.send(LotusProtocol.color(cmd.r, cmd.g, cmd.b), useEnc())
                    is Pending.Bright -> ble.send(LotusProtocol.brightness(cmd.v), useEnc())
                }
            }
        }
        viewModelScope.launch { persistFlow.debounce(500).collect { repo.setLastColor(it) } }
        viewModelScope.launch {
            slotPersist.debounce(400).collect { rgb ->
                val idx = _activeSlot.value ?: return@collect
                savedRepo.setColorSlot(ColorSlot(idx, rgb))
            }
        }
        viewModelScope.launch {
            val s = settings.first()
            if (s.autoConnect && s.lastDeviceMac.isNotBlank() && ble.isBleSupported) {
                AppLogger.i("UI", "Auto-connect to ${s.lastDeviceName}")
                runCatching {
                    val ad = (getApplication<App>())
                        .getSystemService(android.bluetooth.BluetoothManager::class.java).adapter
                    val dev = ad.getRemoteDevice(s.lastDeviceMac)
                    connect(dev)
                }
            }
        }
    }

    private fun useEnc(): Boolean = settings.value.elkEncryption

    // ============ Скан ============

    fun startScanAuto() {
        AppLogger.i("UI", "startScanAuto")
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _scanning.value = true
            try {
                ble.scan().collect { list ->
                    _devices.value = list
                    val target = list.firstOrNull { it.device.name?.startsWith("ELK-", true) == true }
                        ?: list.firstOrNull { it.device.name?.startsWith("LED", true) == true }
                        ?: list.firstOrNull { it.device.name?.contains("BLEDOM", true) == true }
                    if (target != null && connectionState.value !is ConnectionState.Connected) {
                        AppLogger.i("UI", "Auto-connect to ${target.device.name}")
                        _scanning.value = false
                        scanJob?.cancel()
                        connect(target.device)
                    }
                }
            } finally { _scanning.value = false }
        }
    }

    fun stopScan() {
        scanJob?.cancel(); _scanning.value = false
    }

    // ============ Соединение ============

    fun connect(device: BluetoothDevice) = viewModelScope.launch {
        AppLogger.i("UI", "connect ${device.name} / ${device.address}")
        val ok = ble.connect(device)
        if (ok) {
            repo.setLastDevice(device.address, device.name ?: "RGB light")
            val ready = withTimeoutOrNull(3000) { ble.ready.first { it } } ?: false
            AppLogger.i("UI", "ready=$ready")
            val s = settings.value
            val isElk = device.name?.startsWith("ELK-", true) == true || s.elkEncryption
            if (ready && s.timeSync && !isElk) {
                delay(150); ble.send(LotusProtocol.timeSync(), useEnc()); delay(80)
            }
            if (ready) {
                val lc = s.lastColor
                ble.send(LotusProtocol.color(lc.r, lc.g, lc.b), useEnc())
                delay(80)
                ble.send(LotusProtocol.brightness(lc.brightness), useEnc())
            }
            LedService.start(getApplication(), device.name ?: "RGB light", s.elkEncryption)
        }
    }

    fun disconnect() {
        AppLogger.i("UI", "disconnect")
        ble.disconnect()
        LedService.stop(getApplication())
        _isEffectPlaying.value = false
        _isMixPlaying.value = false
        _isMusicPlaying.value = false
        _sleepUntilMs.value = null
    }

    fun togglePower() = viewModelScope.launch {
        _powerOn.value = !_powerOn.value
        ble.send(if (_powerOn.value) LotusProtocol.powerOn() else LotusProtocol.powerOff(), useEnc())
    }

    // ============ Цвет ============

    fun updateColor(hue: Float, sat: Float) {
        _colorState.value = _colorState.value.copy(hue = hue, saturation = sat)
        val rgb = hsvToRgb(hue, sat, 1f)
        pending.tryEmit(Pending.Rgb(rgb[0], rgb[1], rgb[2]))
        persist(rgb)
    }

    fun updateRgb(r: Int, g: Int, b: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)), hsv)
        _colorState.value = _colorState.value.copy(hue = hsv[0], saturation = hsv[1])
        pending.tryEmit(Pending.Rgb(r, g, b))
        persist(intArrayOf(r, g, b))
    }

    fun applyPreset(rgb: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(rgb, hsv)
        _colorState.value = _colorState.value.copy(hue = hsv[0], saturation = hsv[1])
        pending.tryEmit(Pending.Rgb(Color.red(rgb), Color.green(rgb), Color.blue(rgb)))
        persist(intArrayOf(Color.red(rgb), Color.green(rgb), Color.blue(rgb)))
    }

    fun updateBrightness(value: Float) {
        _colorState.value = _colorState.value.copy(value = value)
        pending.tryEmit(Pending.Bright((value.coerceIn(0f, 100f) * 2.55f).toInt().coerceIn(0, 255)))
        viewModelScope.launch { repo.setBrightness(value.toInt()) }
        val rgb = hsvToRgb(_colorState.value.hue, _colorState.value.saturation, 1f)
        persist(rgb, value.toInt())
    }

    fun setSpeed(v: Int) = viewModelScope.launch { repo.setSpeed(v) }
    fun setGlobalBrightness(v: Int) = viewModelScope.launch { repo.setBrightness(v) }
    fun setHoldMs(v: Int) = viewModelScope.launch { repo.setHoldMs(v) }
    fun setRampMs(v: Int) = viewModelScope.launch { repo.setRampMs(v) }

    private fun persist(rgb: IntArray, brightness: Int = _colorState.value.value.toInt()) {
        persistFlow.tryEmit(LastColor(rgb[0], rgb[1], rgb[2], brightness))
        _activeSlot.value?.let { slotPersist.tryEmit(Color.rgb(rgb[0], rgb[1], rgb[2])) }
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val i = Color.HSVToColor(floatArrayOf(h, s, v))
        return intArrayOf(i shr 16 and 0xFF, i shr 8 and 0xFF, i and 0xFF)
    }

    // ============ Слоты ============

    fun activateSlot(index: Int) {
        _activeSlot.value = if (_activeSlot.value == index) null else index
        val slot = colorSlots.value.getOrNull(index)
        if (slot?.rgb != null && _activeSlot.value == index) applyPreset(slot.rgb)
    }
    fun addEmptySlot() {
        val next = colorSlots.value.indexOfFirst { it.rgb == null }
        if (next >= 0) {
            _activeSlot.value = next
            val rgb = Color.HSVToColor(floatArrayOf(
                _colorState.value.hue, _colorState.value.saturation, 1f
            ))
            viewModelScope.launch { savedRepo.setColorSlot(ColorSlot(next, rgb)) }
        }
    }
    fun clearSlot(index: Int) = viewModelScope.launch {
        savedRepo.clearColorSlot(index)
        if (_activeSlot.value == index) _activeSlot.value = null
    }

    // ============ Сохранить цвет ============

    fun saveCurrentColor(name: String) = viewModelScope.launch {
        val rgb = Color.HSVToColor(floatArrayOf(_colorState.value.hue, _colorState.value.saturation, 1f))
        val r = Color.red(rgb); val g = Color.green(rgb); val b = Color.blue(rgb)
        savedRepo.saveColor(SavedColor(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "#%02X%02X%02X".format(r, g, b) },
            r = r, g = g, b = b
        ))
    }

    // ============ Эффекты ============

    fun setEffectMode(m: EffectMode) { _effectMode.value = m }
    fun setEffectColors(c: List<Int>) { _effectColors.value = c }
    fun addEffectColor(rgb: Int) { _effectColors.value = _effectColors.value + rgb }
    fun removeEffectColor(index: Int) {
        _effectColors.value = _effectColors.value.toMutableList().also { it.removeAt(index) }
    }
    fun setProtocolId(id: Int?) { _protocolId.value = id }

    fun playEffect() {
        val s = settings.value
        _protocolId.value?.let { id ->
            _isEffectPlaying.value = true
            LedService.playProtocol(getApplication(), id, s.speed, s.elkEncryption)
            return
        }
        _isEffectPlaying.value = true
        LedService.playEffect(
            getApplication(), _effectMode.value, _effectColors.value,
            s.speed, s.brightness, s.holdMs, s.rampMs, s.elkEncryption
        )
    }
    fun stopEffect() {
        _isEffectPlaying.value = false
        _protocolId.value = null
        LedService.stopEffect(getApplication())
        val rgb = hsvToRgb(_colorState.value.hue, _colorState.value.saturation, 1f)
        ble.send(LotusProtocol.color(rgb[0], rgb[1], rgb[2]), useEnc())
    }

    fun saveCurrentEffect(name: String) = viewModelScope.launch {
        val s = settings.value
        savedRepo.saveEffect(SavedEffect(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank {
                _protocolId.value?.let { id -> EffectCatalog.protocolEffects.firstOrNull { it.first == id }?.second }
                    ?: EffectCatalog.modes.firstOrNull { it.mode == _effectMode.value }?.label
                    ?: "Эффект"
            },
            protocolId = _protocolId.value,
            mode = _effectMode.value,
            colors = _effectColors.value,
            speed = s.speed, brightness = s.brightness,
            holdMs = s.holdMs, rampMs = s.rampMs
        ))
    }

    fun applySavedEffect(e: SavedEffect) {
        viewModelScope.launch {
            repo.setSpeed(e.speed); repo.setBrightness(e.brightness)
            repo.setHoldMs(e.holdMs); repo.setRampMs(e.rampMs)
        }
        _protocolId.value = e.protocolId
        _effectMode.value = e.mode
        _effectColors.value = e.colors
        _isEffectPlaying.value = true
        if (e.protocolId != null)
            LedService.playProtocol(getApplication(), e.protocolId, e.speed, useEnc())
        else
            LedService.playEffect(
                getApplication(), e.mode, e.colors,
                e.speed, e.brightness, e.holdMs, e.rampMs, useEnc()
            )
    }

    // ============ Микс ============

    fun addMixStep(step: MixUiStep) { _mixSteps.value = _mixSteps.value + step }
    fun removeMixStep(i: Int) {
        _mixSteps.value = _mixSteps.value.toMutableList().also { it.removeAt(i) }
    }
    fun updateMixStep(i: Int, step: MixUiStep) {
        val l = _mixSteps.value.toMutableList()
        if (i in l.indices) { l[i] = step; _mixSteps.value = l }
    }
    fun clearMix() { _mixSteps.value = emptyList() }

    fun playMix() {
        if (_mixSteps.value.isEmpty()) return
        val s = settings.value
        _isMixPlaying.value = true
        LedService.playMix(
            getApplication(), _mixSteps.value.map { it.toModel() },
            s.speed, s.brightness, s.elkEncryption
        )
    }
    fun stopMix() {
        _isMixPlaying.value = false
        LedService.stopMix(getApplication())
        val rgb = hsvToRgb(_colorState.value.hue, _colorState.value.saturation, 1f)
        ble.send(LotusProtocol.color(rgb[0], rgb[1], rgb[2]), useEnc())
    }
    fun saveCurrentMix(name: String) = viewModelScope.launch {
        if (_mixSteps.value.isEmpty()) return@launch
        val s = settings.value
        savedRepo.saveMix(SavedMix(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Микс ${savedMixes.value.size + 1}" },
            steps = _mixSteps.value.map { it.toModel() },
            speed = s.speed, brightness = s.brightness
        ))
    }
    fun applySavedMix(m: SavedMix) {
        _mixSteps.value = m.steps.map {
            MixUiStep(it.kind, it.color, it.effectMode, it.colors, it.holdMs, it.rampMs)
        }
        playMix()
    }

    // ============ Сохранённое: применить / удалить ============

    fun applySavedColor(col: SavedColor) {
        applyPreset(Color.rgb(col.r, col.g, col.b))
    }
    fun deleteSavedColor(id: String) = viewModelScope.launch { savedRepo.deleteColor(id) }
    fun deleteSavedEffect(id: String) = viewModelScope.launch { savedRepo.deleteEffect(id) }
    fun deleteSavedMix(id: String) = viewModelScope.launch { savedRepo.deleteMix(id) }

    // ============ Музыка ============

    fun setMusicSource(s: MusicSource) { _musicSource.value = s }
    fun startMusic() {
        _isMusicPlaying.value = true
        when (_musicSource.value) {
            MusicSource.CONTROLLER ->
                LedService.musicOn(getApplication(), useEnc())
            MusicSource.PHONE -> startPhoneMusic()
        }
    }
    fun stopMusic() {
        _isMusicPlaying.value = false
        LedService.musicOff(getApplication())
        stopPhoneMusic()
    }

    // ============ Микрофон телефона ============

    private var micJob: Job? = null
    private val _micLevel = MutableStateFlow(0f)
    val micLevel = _micLevel.asStateFlow()

    private fun startPhoneMusic() {
        micJob?.cancel()
        micJob = viewModelScope.launch {
            val recorder = android.media.AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                44100,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                android.media.AudioRecord.getMinBufferSize(44100,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT) * 2
            )
            try {
                recorder.startRecording()
                val buf = ShortArray(2048)
                var hue = 0f
                while (true) {
                    val n = recorder.read(buf, 0, buf.size)
                    if (n > 0) {
                        var sum = 0L; var peak = 0
                        for (i in 0 until n) { val v = kotlin.math.abs(buf[i].toInt()); sum += v; if (v > peak) peak = v }
                        val level = (sum.toFloat() / n / 32768f).coerceIn(0f, 1f)
                        _micLevel.value = level
                        hue = (hue + 3f) % 360f
                        val bri = (level * 2f).coerceIn(0.1f, 1f)
                        val c = Color.HSVToColor(floatArrayOf(hue, 0.9f, bri))
                        ble.send(
                            LotusProtocol.color(Color.red(c), Color.green(c), Color.blue(c)),
                            useEnc()
                        )
                    }
                    delay(60)
                }
            } finally {
                runCatching { recorder.stop(); recorder.release() }
            }
        }
    }
    private fun stopPhoneMusic() { micJob?.cancel(); micJob = null; _micLevel.value = 0f }

    // ============ Сон-таймер ============

    fun startSleep(minutes: Int) {
        if (minutes <= 0) return
        val until = System.currentTimeMillis() + minutes * 60_000L
        _sleepUntilMs.value = until
        LedService.scheduleSleep(getApplication(), minutes)
        viewModelScope.launch {
            while (System.currentTimeMillis() < until) {
                delay(1000)
                if (_sleepUntilMs.value != until) break
            }
            if (_sleepUntilMs.value == until) {
                _sleepUntilMs.value = null
                _powerOn.value = false
            }
        }
    }
    fun cancelSleep() {
        _sleepUntilMs.value = null
        LedService.scheduleSleep(getApplication(), 0)
    }

    // ============ Настройки ============

    fun setTheme(v: ThemeMode) = viewModelScope.launch { repo.setTheme(v) }
    fun setAutoConnect(v: Boolean) = viewModelScope.launch { repo.setAutoConnect(v) }
    fun setElkEncryption(v: Boolean) = viewModelScope.launch { repo.setElkEncryption(v) }
    fun setTimeSync(v: Boolean) = viewModelScope.launch { repo.setTimeSync(v) }
    fun setThrottle(v: Int) = viewModelScope.launch { repo.setThrottle(v) }
    fun setUpdateUrl(v: String) = viewModelScope.launch { repo.setUpdateUrl(v) }
    fun setDefaultSleep(v: Int) = viewModelScope.launch { repo.setSleepMinutes(v) }

    override fun onCleared() {
        AppLogger.i("UI", "onCleared")
        scanJob?.cancel(); micJob?.cancel()
        super.onCleared()
    }
}