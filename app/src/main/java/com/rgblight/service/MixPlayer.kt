package com.rgblight.service

import android.graphics.Color
import com.rgblight.ble.BleManager
import com.rgblight.ble.LotusProtocol
import com.rgblight.data.EffectMode
import com.rgblight.data.MixStep
import com.rgblight.data.StepKind
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

class MixPlayer(
    private val ble: BleManager,
    private val scope: CoroutineScope,
    private val useEncryption: () -> Boolean
) {
    private var job: Job? = null

    fun play(steps: List<MixStep>, speed: Int, brightness: Int) {
        stop()
        if (steps.isEmpty()) return
        job = scope.launch(Dispatchers.Default) {
            val mul = 2f - (speed.coerceIn(0, 255) / 255f) * 1.6f
            val bri = brightness.coerceIn(0, 100) / 100f
            try {
                while (isActive) {
                    for (step in steps) {
                        if (!isActive) break
                        val hold = (step.holdMs * mul).toLong().coerceAtLeast(80L)
                        val ramp = (step.rampMs * mul).toLong().coerceAtLeast(80L)
                        when (step.kind) {
                            StepKind.COLOR -> {
                                fade(0x000000, step.color, ramp, bri)
                                delay(hold)
                            }
                            StepKind.EFFECT -> {
                                playEffectBlock(step, ramp, hold, bri)
                            }
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    steps.firstOrNull { it.kind == StepKind.COLOR }?.let {
                        val c = it.color
                        ble.send(LotusProtocol.color(Color.red(c), Color.green(c), Color.blue(c)), useEncryption())
                    }
                }
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    private suspend fun playEffectBlock(step: MixStep, ramp: Long, hold: Long, bri: Float) {
        val seq = step.colors.ifEmpty { listOf(0xFF0000, 0x00FF00, 0x0000FF) }
        val end = System.currentTimeMillis() + hold
        var idx = 0
        while (coroutineContext.isActive && System.currentTimeMillis() < end) {
            when (step.effectMode) {
                EffectMode.FADE, EffectMode.FADE_CUSTOM, EffectMode.FADE_PAUSE -> {
                    val a = seq[idx % seq.size]; val b = seq[(idx + 1) % seq.size]
                    fade(a, b, ramp, bri); idx++
                }
                EffectMode.PULSE -> { fade(0x000000, seq[0], ramp, bri); fade(seq[0], 0x000000, ramp, bri) }
                EffectMode.JUMP -> { send(seq[idx % seq.size], bri); delay(ramp.coerceAtLeast(150L)); idx++ }
                EffectMode.NEW_YEAR -> {
                    val r = java.util.Random()
                    send(seq[r.nextInt(seq.size)], bri); delay(ramp.coerceAtLeast(80L))
                }
                EffectMode.FADE_PULSE -> { fade(seq[idx % seq.size], seq[(idx + 1) % seq.size], ramp, bri); idx++ }
            }
        }
    }

    private suspend fun fade(from: Int, to: Int, ms: Long, bri: Float) {
        val steps = (ms / 30).coerceAtLeast(4L)
        val fr = Color.red(from); val fg = Color.green(from); val fb = Color.blue(from)
        val tr = Color.red(to);   val tg = Color.green(to);   val tb = Color.blue(to)
        for (i in 1..steps) {
            val k = i.toFloat() / steps
            withContext(Dispatchers.Main) {
                push(
                    (fr + (tr - fr) * k).toInt(),
                    (fg + (tg - fg) * k).toInt(),
                    (fb + (tb - fb) * k).toInt(),
                    bri
                )
            }
            delay(ms / steps)
        }
    }

    private suspend fun send(c: Int, bri: Float) {
        withContext(Dispatchers.Main) { push(Color.red(c), Color.green(c), Color.blue(c), bri) }
    }

    private fun push(r: Int, g: Int, b: Int, bri: Float) {
        ble.send(
            LotusProtocol.color(
                (r * bri).toInt().coerceIn(0, 255),
                (g * bri).toInt().coerceIn(0, 255),
                (b * bri).toInt().coerceIn(0, 255)
            ),
            useEncryption()
        )
    }
}