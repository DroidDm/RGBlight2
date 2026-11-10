package com.rgblight.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rgb_light_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val ELK_ENC = booleanPreferencesKey("elk_enc")
        val TIME_SYNC = booleanPreferencesKey("time_sync")
        val MIX_BG = stringPreferencesKey("mix_bg")
        val THROTTLE = intPreferencesKey("throttle")
        val LAST_MAC = stringPreferencesKey("last_mac")
        val LAST_NAME = stringPreferencesKey("last_name")
        val LAST_R = intPreferencesKey("last_r")
        val LAST_G = intPreferencesKey("last_g")
        val LAST_B = intPreferencesKey("last_b")
        val LAST_BRIGHT = intPreferencesKey("last_bright")
        val UPDATE_URL = stringPreferencesKey("update_url")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            autoConnect = p[Keys.AUTO_CONNECT] ?: true,
            elkEncryption = p[Keys.ELK_ENC] ?: false,
            timeSync = p[Keys.TIME_SYNC] ?: true,
            mixBackground = runCatching { MixBackgroundMode.valueOf(p[Keys.MIX_BG] ?: "ASK") }.getOrDefault(MixBackgroundMode.ASK),
            throttleMs = p[Keys.THROTTLE] ?: 60,
            lastDeviceMac = p[Keys.LAST_MAC] ?: "",
            lastDeviceName = p[Keys.LAST_NAME] ?: "",
            lastColor = LastColor(
                r = p[Keys.LAST_R] ?: 255,
                g = p[Keys.LAST_G] ?: 255,
                b = p[Keys.LAST_B] ?: 255,
                brightness = p[Keys.LAST_BRIGHT] ?: 100
            ),
            updateUrl = p[Keys.UPDATE_URL] ?: ""
        )
    }

    suspend fun setTheme(v: ThemeMode) { context.dataStore.edit { it[Keys.THEME] = v.name } }
    suspend fun setAutoConnect(v: Boolean) { context.dataStore.edit { it[Keys.AUTO_CONNECT] = v } }
    suspend fun setElkEncryption(v: Boolean) { context.dataStore.edit { it[Keys.ELK_ENC] = v } }
    suspend fun setTimeSync(v: Boolean) { context.dataStore.edit { it[Keys.TIME_SYNC] = v } }
    suspend fun setMixBackground(v: MixBackgroundMode) { context.dataStore.edit { it[Keys.MIX_BG] = v.name } }
    suspend fun setThrottle(v: Int) { context.dataStore.edit { it[Keys.THROTTLE] = v } }
    suspend fun setLastDevice(mac: String, name: String) {
        context.dataStore.edit { it[Keys.LAST_MAC] = mac; it[Keys.LAST_NAME] = name }
    }
    suspend fun clearLastDevice() {
        context.dataStore.edit { it[Keys.LAST_MAC] = ""; it[Keys.LAST_NAME] = "" }
    }
    suspend fun setLastColor(c: LastColor) {
        context.dataStore.edit {
            it[Keys.LAST_R] = c.r; it[Keys.LAST_G] = c.g; it[Keys.LAST_B] = c.b
            it[Keys.LAST_BRIGHT] = c.brightness
        }
    }
    suspend fun setUpdateUrl(v: String) { context.dataStore.edit { it[Keys.UPDATE_URL] = v } }
}