package com.rgblight.data

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class MixBackgroundMode { ASK, ALWAYS, NEVER }

data class LastColor(val r: Int = 255, val g: Int = 255, val b: Int = 255, val brightness: Int = 100)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoConnect: Boolean = true,
    val elkEncryption: Boolean = false,
    val timeSync: Boolean = true,
    val mixBackground: MixBackgroundMode = MixBackgroundMode.ASK,
    val throttleMs: Int = 60,
    val lastDeviceMac: String = "",
    val lastDeviceName: String = "",
    val lastColor: LastColor = LastColor(),
    val updateUrl: String = "",
    val defaultSleepMinutes: Int = 30,
    // Параметры эффектов (общие)
    val speed: Int = 128,
    val brightness: Int = 100,
    val holdMs: Int = 1000,
    val rampMs: Int = 500
)