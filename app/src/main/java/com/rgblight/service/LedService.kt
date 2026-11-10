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
import com.rgblight.data.MixMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class LedService : Service() {

    companion object {
        const val CHANNEL_ID = "rgb_light_led"
        const val NOTIF_ID = 1001
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_MIX_PLAY = "MIX_PLAY"
        const val ACTION_MIX_STOP = "MIX_STOP"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_MIX_COLORS = "mix_colors"
        const val EXTRA_MIX_MODE = "mix_mode"
        const val EXTRA_MIX_SPEED = "mix_speed"
        const val EXTRA_MIX_BRIGHT = "mix_bright"

        fun start(context: Context, deviceName: String) {
            val i = Intent(context, LedService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DEVICE_NAME, deviceName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LedService::class.java))
        }

        fun playMix(context: Context, colors: List<Int>, mode: MixMode, speed: Int, brightness: Int) {
            val i = Intent(context, LedService::class.java).apply {
                action = ACTION_MIX_PLAY
                putExtra(EXTRA_MIX_COLORS, colors.toIntArray())
                putExtra(EXTRA_MIX_MODE, mode.name)
                putExtra(EXTRA_MIX_SPEED, speed)
                putExtra(EXTRA_MIX_BRIGHT, brightness)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stopMix(context: Context) {
            val i = Intent(context, LedService::class.java).apply { action = ACTION_MIX_STOP }
            context.startService(i)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var deviceName: String = "RGB light"
    private val mixPlayer by lazy { MixPlayer((application as App).bleManager, scope) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: deviceName
                startForegroundCompat("Подключено · " + deviceName)
                observeConnection()
            }
            ACTION_MIX_PLAY -> {
                startForegroundCompat("Микс играет · " + deviceName)
                observeConnection()
                val colors = intent.getIntArrayExtra(EXTRA_MIX_COLORS)?.toList() ?: emptyList()
                val mode = runCatching {
                    MixMode.valueOf(intent.getStringExtra(EXTRA_MIX_MODE) ?: "FADE")
                }.getOrDefault(MixMode.FADE)
                val speed = intent.getIntExtra(EXTRA_MIX_SPEED, 128)
                val bright = intent.getIntExtra(EXTRA_MIX_BRIGHT, 100)
                mixPlayer.start(colors, mode, speed, bright)
                updateNotification("Микс играет · " + deviceName)
            }
            ACTION_MIX_STOP -> {
                mixPlayer.stop()
                updateNotification("Подключено · " + deviceName)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun observeConnection() {
        scope.launch {
            (application as App).bleManager.connectionState.collectLatest { state ->
                if (state is ConnectionState.Disconnected) {
                    mixPlayer.stop()
                    stopSelf()
                }
            }
        }
    }

    private fun startForegroundCompat(text: String) {
        val notif = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun updateNotification(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIF_ID, buildNotification(text))
        }
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, LedService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("RGB light")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, "Отключить", stop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHANNEL_ID, "RGB light", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        mixPlayer.stop()
        scope.cancel()
        super.onDestroy()
    }
}