package com.rgblight

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rgblight.ble.ConnectionState
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.components.IosTabBar
import com.rgblight.ui.components.IosTopBar
import com.rgblight.ui.screens.*
import com.rgblight.ui.theme.RgbLightTheme
import com.rgblight.ui.theme.iosColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Root() }
    }
}

@Composable
private fun Root(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    RgbLightTheme(mode = settings.themeMode) { AppScreen(vm) }
}

@Composable
private fun AppScreen(vm: MainViewModel) {
    val c = iosColors()
    val state by vm.connectionState.collectAsState()
    val connected by vm.connectedDevice.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val sleepUntil by vm.sleepUntilMs.collectAsState()
    val settings by vm.settings.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var showSleep by remember { mutableStateOf(false) }
    var permsAsked by remember { mutableStateOf(false) }

    val perms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (!permsAsked) {
            permsAsked = true
            val need = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                need += Manifest.permission.BLUETOOTH_SCAN
                need += Manifest.permission.BLUETOOTH_CONNECT
            } else need += Manifest.permission.ACCESS_FINE_LOCATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                need += Manifest.permission.POST_NOTIFICATIONS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                need += Manifest.permission.RECORD_AUDIO
            perms.launch(need.toTypedArray())
        }
    }

    val statusColor = when (state) {
        is ConnectionState.Connected -> c.green
        is ConnectionState.Connecting -> Color(0xFFFFCC00)
        else -> c.textSecondary
    }
    val statusText = when (state) {
        is ConnectionState.Connected -> connected?.name ?: "Подключено"
        is ConnectionState.Connecting -> "Подключение…"
        else -> "Не подключено"
    }

    // Сон-таймер: остаток в мм:сс
    val sleepText = sleepUntil?.let { until ->
        val left = (until - System.currentTimeMillis()).coerceAtLeast(0)
        "%d:%02d".format(left / 60_000, left / 1000 % 60)
    }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            IosTopBar(
                title = "RGB light",
                subtitle = if (sleepText != null) "Сон: $sleepText" else statusText,
                statusColor = if (sleepText != null) Color(0xFFFF9500) else statusColor,
                rightActions = {
                    IconButton(onClick = { vm.startScanAuto() }) {
                        if (scanning) CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = c.accent
                        ) else Icon(Icons.Default.Search, null, tint = c.textPrimary)
                    }
                    IconButton(onClick = { vm.togglePower() }) {
                        Icon(Icons.Default.PowerSettingsNew, null, tint = c.textPrimary)
                    }
                    IconButton(onClick = { vm.disconnect() }) {
                        Icon(Icons.Default.BluetoothDisabled, null, tint = c.textPrimary)
                    }
                    IconButton(onClick = { showSleep = true }) {
                        Icon(Icons.Default.Schedule, null, tint = c.textPrimary)
                    }
                }
            )
        },
        bottomBar = {
            IosTabBar(
                items = listOf(
                    Triple("Цвет", Icons.Default.Palette, true),
                    Triple("Эффекты", Icons.Default.AutoAwesome, true),
                    Triple("Микс", Icons.Default.Tune, true),
                    Triple("Музыка", Icons.Default.MusicNote, true),
                    Triple("Сохр.", Icons.Default.Star, true),
                    Triple("Настр.", Icons.Default.Settings, true)
                ),
                selected = tab,
                onSelect = { tab = it }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> ColorScreen(vm)
                1 -> EffectsScreen(vm)
                2 -> MixScreen(vm)
                3 -> MusicScreen(vm)
                4 -> SavedScreen(vm)
                5 -> SettingsScreen(vm)
            }
        }
    }

    if (showSleep) {
        SleepDialog(
            initial = settings.defaultSleepMinutes,
            onStart = { m -> vm.startSleep(m); showSleep = false },
            onCancel = { vm.cancelSleep(); showSleep = false },
            onDismiss = { showSleep = false },
            active = sleepUntil != null
        )
    }
}

@Composable
private fun SleepDialog(
    initial: Int, onStart: (Int) -> Unit, onCancel: () -> Unit,
    onDismiss: () -> Unit, active: Boolean
) {
    val c = iosColors()
    var minutes by remember { mutableStateOf(initial.toString()) }
    val presets = listOf(5, 15, 30, 60)
    com.rgblight.ui.components.IosAlertDialog(
        title = "Сон-таймер",
        message = if (active) "Таймер активен" else "Через сколько выключить ленту",
        confirmLabel = "Запустить",
        dismissLabel = "Отмена",
        onConfirm = { onStart(minutes.toIntOrNull() ?: initial) },
        onDismiss = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.forEach { m ->
                    OutlinedButton(onClick = { minutes = m.toString() }, modifier = Modifier.weight(1f)) {
                        Text("$m", color = c.textPrimary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it.filter { ch -> ch.isDigit() }.take(4) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Минут", color = c.textSecondary) }
            )
            if (active) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel) { Text("Остановить таймер", color = c.red) }
            }
        }
    }
}