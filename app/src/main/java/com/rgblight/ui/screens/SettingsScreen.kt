package com.rgblight.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import com.rgblight.BuildConfig
import com.rgblight.data.MixBackgroundMode
import com.rgblight.data.ThemeMode
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.components.*
import com.rgblight.ui.theme.iosColors
import com.rgblight.util.AppLogger
import com.rgblight.util.LogEntry
import com.rgblight.util.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val c = iosColors()
    val settings by vm.settings.collectAsState()
    val logEntries by AppLogger.entries.collectAsState()

    var showLog by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    var showVersion by remember { mutableStateOf(false) }
    var showDisconnect by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val saveLog = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(AppLogger.export().toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Журнал сохранён", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionTitle("Оформление")
        IosCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Тема", color = c.textPrimary, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                IosSegmentedControl(
                    options = listOf("Системная", "Светлая", "Тёмная"),
                    selectedIndex = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> 0; ThemeMode.LIGHT -> 1; else -> 2
                    },
                    onSelect = { i -> vm.setTheme(when (i) { 0 -> ThemeMode.SYSTEM; 1 -> ThemeMode.LIGHT; else -> ThemeMode.DARK }) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("Устройство")
        IosCard {
            IosRow(title = "Автоподключение", subtitle = "Подключаться при запуске",
                trailing = { Switch(settings.autoConnect, { vm.setAutoConnect(it) }) })
            IosDivider()
            IosRow(title = "Шифрование ELK", subtitle = "Только если цвета не работают",
                trailing = { Switch(settings.elkEncryption, { vm.setElkEncryption(it) }) })
            IosDivider()
            IosRow(title = "Синхронизация времени",
                trailing = { Switch(settings.timeSync, { vm.setTimeSync(it) }) })
            IosDivider()
            IosRow(title = "Отключиться от устройства", trailing = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textSecondary)
            }, onClick = { showDisconnect = true })
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("Сон-таймер по умолчанию")
        IosCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                IosSegmentedControl(
                    options = listOf("5", "15", "30", "60"),
                    selectedIndex = when (settings.defaultSleepMinutes) {
                        5 -> 0; 15 -> 1; 60 -> 3; else -> 2
                    },
                    onSelect = { i -> vm.setDefaultSleep(listOf(5, 15, 30, 60)[i]) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("Bluetooth")
        IosCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Throttle", color = c.textPrimary, fontSize = 17.sp)
                    Text("${settings.throttleMs} мс", color = c.textSecondary, fontSize = 15.sp)
                }
                Spacer(Modifier.height(8.dp))
                IosSegmentedControl(
                    options = listOf("30", "60", "120"),
                    selectedIndex = when (settings.throttleMs) { 30 -> 0; 120 -> 2; else -> 1 },
                    onSelect = { i -> vm.setThrottle(listOf(30, 60, 120)[i]) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("Диагностика")
        IosCard {
            IosRow(title = "Открыть журнал", subtitle = "${logEntries.size} записей",
                trailing = { Icon(Icons.Default.List, null, tint = c.accent) },
                onClick = { showLog = true })
            IosDivider()
            IosRow(title = "Сохранить журнал в TXT",
                trailing = { Icon(Icons.Default.Save, null, tint = c.accent) },
                onClick = {
                    val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    saveLog.launch("rgb_light_log_${df.format(Date())}.txt")
                })
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("О приложении")
        IosCard {
            IosRow(title = "Версия", subtitle = BuildConfig.VERSION_NAME,
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textSecondary) },
                onClick = { showVersion = true })
            IosDivider()
            IosRow(title = "Обновить", subtitle = "Скачать APK по ссылке",
                trailing = { Icon(Icons.Default.Refresh, null, tint = c.accent) },
                onClick = { showUpdate = true })
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showLog) LogDialog(logEntries, onClose = { showLog = false }, onSave = {
        val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        saveLog.launch("rgb_light_log_${df.format(Date())}.txt")
    }, onClear = { AppLogger.clear() })

    if (showVersion) IosAlertDialog(
        title = "RGB light",
        message = "Версия ${BuildConfig.VERSION_NAME}\nСборка ${BuildConfig.VERSION_CODE}",
        confirmLabel = "Закрыть", dismissLabel = null,
        onConfirm = { showVersion = false }, onDismiss = { showVersion = false }
    )
    if (showDisconnect) IosAlertDialog(
        title = "Отключиться?",
        message = "Текущее соединение с подсветкой будет разорвано.",
        destructive = true, confirmLabel = "Отключиться", dismissLabel = "Отмена",
        onConfirm = { vm.disconnect(); showDisconnect = false },
        onDismiss = { showDisconnect = false }
    )
    if (showUpdate) UpdateDialog(
        savedUrl = settings.updateUrl,
        onSave = { vm.setUpdateUrl(it); showUpdate = false },
        onDismiss = { showUpdate = false }
    )
}

@Composable
private fun SectionTitle(text: String) {
    val c = iosColors()
    Text(text.uppercase(), color = c.textSecondary, fontSize = 13.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
}

@Composable
private fun UpdateDialog(savedUrl: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val c = iosColors()
    var url by remember { mutableStateOf(savedUrl) }
    var err by remember { mutableStateOf<String?>(null) }
    IosAlertDialog(
        title = "Ссылка на обновление",
        confirmLabel = "Сохранить", dismissLabel = "Отмена",
        onConfirm = {
            val t = url.trim()
            if (t.isEmpty()) { onSave(""); return@IosAlertDialog }
            if (!t.startsWith("https://") && !t.startsWith("http://")) { err = "Нужен http(s)://"; return@IosAlertDialog }
            if (!t.endsWith(".apk", true)) { err = "Нужна .apk"; return@IosAlertDialog }
            onSave(t)
        },
        onDismiss = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = url, onValueChange = { url = it; err = null },
                singleLine = true, isError = err != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://...app.apk", color = c.textSecondary) }
            )
            if (err != null) { Spacer(Modifier.height(4.dp)); Text(err!!, color = c.red, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun LogDialog(
    entries: List<LogEntry>, onClose: () -> Unit, onSave: () -> Unit, onClear: () -> Unit
) {
    val c = iosColors()
    val clipboard = LocalClipboardManager.current
    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = c.bg) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Журнал", color = c.textPrimary, fontSize = 17.sp,
                        modifier = Modifier.weight(1f))
                    TextButton({ clipboard.setText(AnnotatedString(AppLogger.export())) }) { Text("Копир.", color = c.accent) }
                    TextButton(onSave) { Text("TXT", color = c.accent) }
                    TextButton(onClose) { Text("Закрыть", color = c.accent) }
                }
                HorizontalDivider(color = c.separator.copy(alpha = 0.5f))
                LazyColumn(Modifier.weight(1f).padding(8.dp)) {
                    items(entries.asReversed()) { e ->
                        val col = when (e.level) {
                            LogLevel.DEBUG -> c.textSecondary
                            LogLevel.INFO -> c.textPrimary
                            LogLevel.WARN -> Color(0xFFFF9500)
                            LogLevel.ERROR -> c.red
                        }
                        Text(e.formatted(), color = col, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${entries.size} записей", color = c.textSecondary, fontSize = 12.sp)
                    TextButton(onClear) { Text("Очистить", color = c.red) }
                }
            }
        }
    }
}