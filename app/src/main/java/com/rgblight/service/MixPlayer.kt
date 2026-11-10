package com.rgblight.service

import android.graphics.Color as AndroidColor
import com.rgblight.ble.BleManager
import com.rgblight.ble.LotusProtocol
import com.rgblight.data.MixMode
import kotlinx.coroutines.*

/**
 * Крутит последовательность цветов с учётом режима.
 * Работает внутри LedService, поэтому продолжает играть
 * даже когда приложение свёрнуто.
 */
class MixPlayer(
    private val ble: BleManager,
    private val scope: CoroutineScope
) {

    private var job: Job? = null

    private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlaying = _isPlaying

    fun start(colors: List<Int>, mode: MixMode, speed: Int, brightness: Int) {
        stop()
        if (colors.isEmpty()) return
        _isPlaying.value = true
        job = scope.launch(Dispatchers.Default) {
            val stepMs = speedToMs(speed)
            var idx = 0
            try {
                while (isActive) {
                    val c = colors[idx % colors.size]
                    val r = AndroidColor.red(c)
                    val g = AndroidColor.green(c)
                    val b = AndroidColor.blue(c)
                    withContext(Dispatchers.Main) {
                        ble.send(LotusProtocol.color(r, g, b))
                    }
                    when (mode) {
                        MixMode.STROBE -> {
                            delay((stepMs / 3).toLong())
                            withContext(Dispatchers.Main) {
                                ble.send(LotusProtocol.color(0, 0, 0))
                            }
                            delay((stepMs / 3).toLong())
                        }
                        MixMode.BREATHE -> {
                            delay((stepMs * 2).toLong())
                        }
                        else -> delay(stepMs.toLong())
                    }
                    idx++
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (ble.isBleSupported) {
                        ble.send(LotusProtocol.color(
                            AndroidColor.red(colors.firstOrNull() ?: 0xFFFFFF),
                            AndroidColor.green(colors.firstOrNull() ?: 0xFFFFFF),
                            AndroidColor.blue(colors.firstOrNull() ?: 0xFFFFFF)
                        ))
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _isPlaying.value = false
    }

    private fun speedToMs(speed: Int): Int {
        // speed 0..255: 0 → 3000 ms, 255 → 120 ms
        val s = speed.coerceIn(0, 255)
        return 3000 - (s * (3000 - 120) / 255)
    }
}