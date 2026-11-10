package com.rgblight

import android.app.Application
import com.rgblight.ble.BleManager
import com.rgblight.data.SavedRepository
import com.rgblight.data.SettingsRepository

class App : Application() {
    lateinit var bleManager: BleManager
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var saved: SavedRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        saved = SavedRepository(this)
        bleManager = BleManager(this)
    }
}