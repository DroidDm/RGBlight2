package com.rgblight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rgblight.App
import com.rgblight.MainActivity
import com.rgblight.R
import com.rgblight.ble.ConnectionState
import com.rgblight.data.EffectMode
import com.rgblight.data.MixStep
import com.rgblight.data.StepKind
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class LedService : Service() {

    companion object {
        const val CH = "rgb_light_led"
        const val NID = 1001
        const val A_START = "START"
        const val A_STOP = "STOP"
        const val A_EFFECT = "EFFECT"
        const val A_EFFECT_STOP = "EFFECT_STOP"
        const val A_PROTO = "PROTO"
        const val A_MIX = "MIX"
        const val A_MIX_STOP = "MIX_STOP"
        const val A_MUSIC_ON = "MUSIC_ON"
        const val A_MUSIC_OFF = "MUSIC_OFF"
        const val A_SLEEP = "SLEEP"

        const val E_NAME = "name"
        const val E_ENC = "enc"
        const val E_MODE = "mode"
        const val E_COLORS = "colors"
        const val E_SPEED = "speed"
        const val E_BRIGHT = "bright"
        const val E_HOLD = "hold"
        const val E_RAMP = "ramp"
        const val E_PROTO_ID = "proto_id"
        const val E_STEPS_KIND = "steps_kind"
        const val E_STEPS_COLOR = "steps_color"
        const val E_STEPS_MODE = "steps_mode"
        const val E_STEPS_COLORS = "steps_colors"
        const val E_STEPS_HOLD = "steps_hold"
        const val E_STEPS_RAMP = "steps_ramp"
        const val E_SLEEP_MIN = "sleep_min"

        fun start(ctx: Context, name: String, enc: Boolean) {
            val i = Intent(ctx, LedService::class.java).apply {
                action = A_START
                putExtra(E_NAME, name); putExtra(E_ENC, enc)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, LedService::class.java)) }

        fun playEffect(ctx: Context, mode: EffectMode, colors: List<Int>,
                       speed: Int, brightness: Int, hold: Int, ramp: Int, enc: Boolean) {
            val i = Intent(ctx, LedService::class.java).apply {
                action = A_EFFECT
                putExtra(E_MODE, mode.name)
                putExtra(E_COLORS, colors.toIntArray())
                putExtra(E_SPEED, speed); putExtra(E_BRIGHT, brightness)
                putExtra(E_HOLD, hold); putExtra(E_RAMP, ramp); putExtra(E_ENC, enc)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stopEffect(ctx: Context) {
            ctx.startService(Intent(ctx, LedService::class.java).apply { action = A_EFFECT_STOP })
        }
        fun playProtocol(ctx: Context, id: Int, speed: Int, enc: Boolean) {
            val i = Intent(ctx, LedService::class.java).apply {
                action = A_PROTO
                putExtra(E_PROTO_ID, id); putExtra(E_SPEED, speed); putExtra(E_ENC, enc)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun playMix(ctx: Context, steps: List<MixStep>, speed: Int, brightness: Int, enc: Boolean) {
            val i = Intent(ctx, LedService::class.java).apply {
                action = A_MIX
                putExtra(E_STEPS_KIND, steps.map { it.kind.name }.toTypedArray())
                putExtra(E_STEPS_COLOR, steps.map { it.color }.toIntArray())
                putExtra(E_STEPS_MODE, steps.map { it.effectMode.name }.toTypedArray())
                putExtra(E_STEPS_COLORS, steps.map { s -> s.colors.joinToString(",") }.toTypedArray())
                putExtra(E_STEPS_HOLD, steps.map { it.holdMs }.toIntArray())
                putExtra(E_STEPS_RAMP, steps.map { it.rampMs }.toIntArray())
                putExtra(E_SPEED, speed); putExtra(E_BRIGHT, brightness); putExtra(E_ENC, enc)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stopMix(ctx: Context) {
            ctx.startService(Intent(ctx, LedService::class.java).apply { action = A_MIX_STOP })
        }
        fun musicOn(ctx: Context, enc: Boolean) {
            val i = Intent(ctx, LedService::class.java).apply { action = A_MUSIC_ON; putExtra(E_ENC, enc) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun musicOff(ctx: Context) {
            ctx.startService(Intent(ctx, LedService::class.java).apply { action = A_MUSIC_OFF })
        }
        fun scheduleSleep(ctx: Context, minutes: Int) {
            ctx.startService(Intent(ctx, LedService::class.java).apply {
                action = A_SLEEP; putExtra(E_SLEEP_MIN, minutes)
            })
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var name = "RGB light"
    private var enc = false
    private var sleepJob: Job? = null

    private val effectPlayer by lazy {
        EffectPlayer((application as App).bleManager, scope) { enc }
    }
    private val mixPlayer by lazy {
        MixPlayer((application as App).bleManager, scope) { enc }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() { super.onCreate(); createCh() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ble = (application as App).bleManager
        when (intent?.action) {
            A_START -> {
                name = intent.getStringExtra(E_NAME) ?: name
                enc = intent.getBooleanExtra(E_ENC, false)
                fg("Подключено · $name")
                observe()
            }
            A_EFFECT -> {
                fg("Эффект · $name")
                observe()
                effectPlayer.stop(); mixPlayer.stop()
                val mode = runCatching { EffectMode.valueOf(intent.getStringExtra(E_MODE) ?: "FADE") }.getOrDefault(EffectMode.FADE)
                val colors = intent.getIntArrayExtra(E_COLORS)?.toList() ?: listOf(0xFF0000, 0x00FF00, 0x0000FF)
                effectPlayer.play(
                    mode, colors,
                    intent.getIntExtra(E_SPEED, 128), intent.getIntExtra(E_BRIGHT, 100),
                    intent.getIntExtra(E_HOLD, 1000), intent.getIntExtra(E_RAMP, 500)
                )
                notify("Эффект · $name")
            }
            A_EFFECT_STOP -> { effectPlayer.stop(); notify("Подключено · $name") }
            A_PROTO -> {
                fg("Эффект · $name"); observe()
                effectPlayer.stop(); mixPlayer.stop()
                val id = intent.getIntExtra(E_PROTO_ID, 0)
                val speed = intent.getIntExtra(E_SPEED, 128)
                ble.send(com.rgblight.ble.LotusProtocol.brightness(255), enc)
                ble.send(com.rgblight.ble.LotusProtocol.effect(id, speed), enc)
            }
            A_MIX -> {
                fg("Микс · $name"); observe()
                effectPlayer.stop(); mixPlayer.stop()
                val steps = decodeSteps(intent)
                mixPlayer.play(steps, intent.getIntExtra(E_SPEED, 128), intent.getIntExtra(E_BRIGHT, 100))
                notify("Микс · $name")
            }
            A_MIX_STOP -> { mixPlayer.stop(); notify("Подключено · $name") }
            A_MUSIC_ON -> {
                fg("Музыка · $name"); observe()
                enc = intent.getBooleanExtra(E_ENC, false)
                ble.send(com.rgblight.ble.LotusProtocol.music(true), enc)
            }
            A_MUSIC_OFF -> ble.send(com.rgblight.ble.LotusProtocol.music(false), enc)
            A_SLEEP -> {
                val min = intent.getIntExtra(E_SLEEP_MIN, 30)
                sleepJob?.cancel()
                sleepJob = scope.launch {
                    delay(min * 60_000L)
                    ble.send(com.rgblight.ble.LotusProtocol.powerOff(), enc)
                    notify("Подключено · $name")
                }
            }
            A_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun decodeSteps(i: Intent): List<MixStep> {
        val kinds = i.getStringArrayExtra(E_STEPS_KIND) ?: return emptyList()
        val colors = i.getIntArrayExtra(E_STEPS_COLOR) ?: IntArray(0)
        val modes = i.getStringArrayExtra(E_STEPS_MODE) ?: emptyArray()
        val colorsLists = i.getStringArrayExtra(E_STEPS_COLORS) ?: emptyArray()
        val holds = i.getIntArrayExtra(E_STEPS_HOLD) ?: IntArray(0)
        val ramps = i.getIntArrayExtra(E_STEPS_RAMP) ?: IntArray(0)
        return kinds.indices.map { idx ->
            MixStep(
                kind = runCatching { StepKind.valueOf(kinds[idx]) }.getOrDefault(StepKind.COLOR),
                color = colors.getOrElse(idx) { 0xFFFFFF },
                effectMode = runCatching { EffectMode.valueOf(modes.getOrElse(idx) { "FADE" }) }.getOrDefault(EffectMode.FADE),
                colors = colorsLists.getOrElse(idx) { "" }.split(",").mapNotNull { it.toIntOrNull() }
                    .ifEmpty { listOf(0xFF0000, 0x00FF00, 0x0000FF) },
                holdMs = holds.getOrElse(idx) { 1000 },
                rampMs = ramps.getOrElse(idx) { 500 }
            )
        }
    }

    private fun observe() {
        scope.launch {
            (application as App).bleManager.connectionState.collectLatest { st ->
                if (st is ConnectionState.Disconnected) {
                    effectPlayer.stop(); mixPlayer.stop(); stopSelf()
                }
            }
        }
    }

    private fun fg(text: String) {
        val n = build(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        else startForeground(NID, n)
    }
    private fun notify(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getSystemService(NotificationManager::class.java).notify(NID, build(text))
    }
    private fun build(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("RGB light")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
    private fun createCh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CH, "RGB light", NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        effectPlayer.stop(); mixPlayer.stop(); sleepJob?.cancel()
        scope.cancel(); super.onDestroy()
    }
}