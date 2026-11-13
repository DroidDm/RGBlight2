package com.rgblight.service

import android.graphics.Color
import com.rgblight.ble.BleManager
import com.rgblight.ble.LotusProtocol
import com.rgblight.data.EffectMode
import kotlinx.coroutines.*

/**
 * Генерирует кадры для кастомных эффектов и отправляет их на ленту.
 * Скорость влияет на длительность ramp/hold. Яркость вписана в цвет (color = v * k).
 */
class EffectPlayer(
    private val ble: BleManager,
    private val scope: CoroutineScope,
    private val useEncryption: () -> Boolean
) {
    private var job: Job? = null

    fun play(
        mode: EffectMode,
        colors: List<Int>,
        speed: Int,
        brightness: Int,
        holdMs: Int,
        rampMs: Int
    ) {
        stop()
        if (colors.isEmpty()) return
        job = scope.launch(Dispatchers.Default) {
            val mul = speedMul(speed)
            val hold = (holdMs * mul).toLong().coerceAtLeast(80L)
            val ramp = (rampMs * mul).toLong().coerceAtLeast(80L)
            val bri = brightness.coerceIn(0, 100) / 100f
            try {
                when (mode) {
                    EffectMode.FADE, EffectMode.FADE_CUSTOM, EffectMode.FADE_PAUSE, EffectMode.FADE_PULSE -> {
                        var idx = 0
                        val seq = if (colors.size == 1) listOf(colors[0], 0x000000) else colors
                        while (isActive) {
                            val from = seq[idx % seq.size]
                            val to = seq[(idx + 1) % seq.size]
                            if (mode == EffectMode.FADE_PULSE) {
                                sendColor(from, bri)
                                fade(from, dark(from, 0.5f), ramp / 2, bri)
                                fade(dark(from, 0.5f), from, ramp / 2, bri)
                                delay(hold / 4)
                                fade(from, dark(from, 0.5f), ramp / 2, bri)
                                fade(dark(from, 0.5f), from, ramp / 2, bri)
                            } else {
                                fade(from, to, ramp, bri)
                            }
                            delay(hold)
                            idx++
                        }
                    }
                    EffectMode.PULSE -> {
                        val c = colors[0]
                        while (isActive) {
                            fade(0x000000, c, ramp, bri)
                            delay(hold)
                            fade(c, 0x000000, ramp, bri)
                            delay(hold / 2)
                        }
                    }
                    EffectMode.JUMP -> {
                        var idx = 0
                        while (isActive) {
                            sendColor(colors[idx % colors.size], bri)
                            delay(hold)
                            idx++
                        }
                    }
                    EffectMode.NEW_YEAR -> {
                        val palette = listOf(0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF, 0xFFFFFF)
                        val rnd = java.util.Random()
                        while (isActive) {
                            val c1 = palette[rnd.nextInt(palette.size)]
                            val c2 = palette[rnd.nextInt(palette.size)]
                            sendColor(c1, bri)
                            delay((hold / 3).coerceAtLeast(60L))
                            sendColor(c2, bri)
                            delay((hold / 3).coerceAtLeast(60L))
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    colors.firstOrNull()?.let { sendColor(it, bri) }
                }
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
    }

    private suspend fun fade(from: Int, to: Int, ms: Long, bri: Float) {
        val steps = (ms / 30).coerceAtLeast(4L)
        val fr = Color.red(from); val fg = Color.green(from); val fb = Color.blue(from)
        val tr = Color.red(to);   val tg = Color.green(to);   val tb = Color.blue(to)
        for (i in 1..steps) {
            val k = i.toFloat() / steps
            val r = (fr + (tr - fr) * k).toInt()
            val g = (fg + (tg - fg) * k).toInt()
            val b = (fb + (tb - fb) * k).toInt()
            withContext(Dispatchers.Main) { push(r, g, b, bri) }
            delay(ms / steps)
        }
    }

    private suspend fun sendColor(c: Int, bri: Float) {
        withContext(Dispatchers.Main) {
            push(Color.red(c), Color.green(c), Color.blue(c), bri)
        }
    }

    private fun push(r: Int, g: Int, b: Int, bri: Float) {
        val rr = (r * bri).toInt().coerceIn(0, 255)
        val gg = (g * bri).toInt().coerceIn(0, 255)
        val bb = (b * bri).toInt().coerceIn(0, 255)
        ble.send(LotusProtocol.color(rr, gg, bb), useEncryption())
    }

    private fun dark(c: Int, k: Float): Int =
        Color.rgb((Color.red(c) * k).toInt(), (Color.green(c) * k).toInt(), (Color.blue(c) * k).toInt())

    private fun speedMul(speed: Int): Float {
        val s = speed.coerceIn(0, 255)
        return 2f - (s / 255f) * 1.6f   // speed=0 → 2x медленнее, speed=255 → 0.4x
    }
}