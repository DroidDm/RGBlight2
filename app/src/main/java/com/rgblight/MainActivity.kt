package com.rgblight

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    RgbLightTheme(mode = settings.themeMode) {
        AppScreen(vm)
    }
}

@Composable
private fun AppScreen(vm: MainViewModel) {
    val c = iosColors()
    val state by vm.connectionState.collectAsState()
    val connected by vm.connectedDevice.collectAsState()
    val devices by vm.devices.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var permsAsked by remember { mutableStateOf(false) }

    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (!permsAsked) {
            permsAsked = true
            val needed = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                needed.add(Manifest.permission.BLUETOOTH_SCAN)
                needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permsLauncher.launch(needed.toTypedArray())
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

    Scaffold(
        containerColor = c.bg,
        topBar = {
            IosTopBar(
                title = "RGB light",
                subtitle = statusText,
                statusColor = statusColor,
                rightActions = {
                    IconButton(onClick = { vm.togglePower() }) {
                        Icon(Icons.Default.PowerSettingsNew, null, tint = c.textPrimary)
                    }
                    IconButton(onClick = { vm.disconnect() }) {
                        Icon(Icons.Default.BluetoothDisabled, null, tint = c.textPrimary)
                    }
                }
            )
        },
        bottomBar = {
            IosTabBar(
                items = listOf(
                    "Цвет" to Icons.Default.Palette,
                    "Эффекты" to Icons.Default.AutoAwesome,
                    "Микс" to Icons.Default.Tune,
                    "Сохр." to Icons.Default.Star,
                    "Настройки" to Icons.Default.Settings
                ),
                selected = tab,
                onSelect = { tab = it }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val notConnected = state !is ConnectionState.Connected
            if (notConnected && tab == 0) {
                ConnectPrompt(
                    devices = devices.map { (it.device.name ?: it.device.address) to it.device },
                    onScan = { vm.startScan() },
                    onConnect = { vm.connect(it) }
                )
            } else {
                when (tab) {
                    0 -> ColorScreen(vm)
                    1 -> EffectsScreen(vm)
                    2 -> MixScreen(vm)
                    3 -> SavedScreen(vm)
                    4 -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun ConnectPrompt(
    devices: List<Pair<String, BluetoothDevice>>,
    onScan: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit
) {
    val c = iosColors()
    Column(
        Modifier.fillMaxSize().background(c.bg).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("🔍", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Подключитесь к подсветке", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Включите подсветку и нажмите «Сканировать»", color = c.textSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onScan,
            colors = ButtonDefaults.buttonColors(containerColor = c.accent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(50.dp).fillMaxWidth()
        ) { Text("Сканировать", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(devices) { (name, dev) ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(c.surface, RoundedCornerShape(10.dp))
                        .clickable { onConnect(dev) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = c.textPrimary, fontSize = 17.sp)
                        Text(dev.address, color = c.textSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = c.textSecondary)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}