package com.rgblight.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun suggestedLogName(): String {
        val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "rgb_light_log_${df.format(Date())}.txt"
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(AppLogger.export().toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Журнал сохранён", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        SectionTitle("Оформление")
        IosCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Тема", color = c.textPrimary, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                IosSegmentedControl(
                    options = listOf("Системная", "Светлая", "Тёмная"),
                    selectedIndex = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> 0
                        ThemeMode.LIGHT -> 1
                        ThemeMode.DARK -> 2
                    },
                    onSelect = { i ->
                        vm.setTheme(when (i) {
                            0 -> ThemeMode.SYSTEM
                            1 -> ThemeMode.LIGHT
                            else -> ThemeMode.DARK
                        })
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("Устройство")
        IosCard {
            IosRow(
                title = "Автоподключение",
                subtitle = "Подключаться при запуске",
                trailing = { Switch(settings.autoConnect, { vm.setAutoConnect(it) }) }
            )
            IosDivider()
            IosRow(
                title = "Шифрование ELK",
                subtitle = "Если цвета не работают",
                trailing = { Switch(settings.elkEncryption, { vm.setElkEncryption(it) }) }
            )
            IosDivider()
            IosRow(
                title = "Синхронизация времени",
                subtitle = "Для таймеров на контроллере",
                trailing = { Switch(settings.timeSync, { vm.setTimeSync(it) }) }
            )
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("Микс в фоне")
        IosCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Поведение при выходе", color = c.textPrimary, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                IosSegmentedControl(
                    options = listOf("Спрашивать", "Всегда", "Никогда"),
                    selectedIndex = when (settings.mixBackground) {
                        MixBackgroundMode.ASK -> 0
                        MixBackgroundMode.ALWAYS -> 1
                        MixBackgroundMode.NEVER -> 2
                    },
                    onSelect = { i ->
                        vm.setMixBackground(when (i) {
                            0 -> MixBackgroundMode.ASK
                            1 -> MixBackgroundMode.ALWAYS
                            else -> MixBackgroundMode.NEVER
                        })
                    }
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
                    selectedIndex = when (settings.throttleMs) {
                        30 -> 0
                        120 -> 2
                        else -> 1
                    },
                    onSelect = { i -> vm.setThrottle(when (i) { 0 -> 30; 2 -> 120; else -> 60 }) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("Диагностика")
        IosCard {
            IosRow(
                title = "Открыть журнал",
                subtitle = "${logEntries.size} записей · лог BLE и команд",
                trailing = { Icon(Icons.Default.List, null, tint = c.accent) },
                onClick = { showLogDialog = true }
            )
            IosDivider()
            IosRow(
                title = "Сохранить журнал в TXT",
                subtitle = "Выбрать место сохранения",
                trailing = { Icon(Icons.Default.Save, null, tint = c.accent) },
                onClick = { saveLauncher.launch(suggestedLogName()) }
            )
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("О приложении")
        IosCard {
            IosRow(
                title = "Версия",
                subtitle = BuildConfig.VERSION_NAME,
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textSecondary) },
                onClick = { showVersionDialog = true }
            )
            IosDivider()
            IosRow(
                title = "Обновить",
                subtitle = "Скачать APK по ссылке",
                trailing = { Icon(Icons.Default.Refresh, null, tint = c.accent) },
                onClick = { showUpdateDialog = true }
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showLogDialog) {
        LogDialog(
            entries = logEntries,
            onClose = { showLogDialog = false },
            onSave = { saveLauncher.launch(suggestedLogName()) },
            onClear = { AppLogger.clear() }
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(
            savedUrl = settings.updateUrl,
            onSave = { vm.setUpdateUrl(it); showUpdateDialog = false },
            onDismiss = { showUpdateDialog = false }
        )
    }
    if (showVersionDialog) {
        IosAlertDialog(
            title = "RGB light",
            message = "Версия ${BuildConfig.VERSION_NAME}\nСборка ${BuildConfig.VERSION_CODE}",
            confirmLabel = "Закрыть",
            dismissLabel = null,
            onConfirm = { showVersionDialog = false },
            onDismiss = { showVersionDialog = false }
        )
    }
}

@Composable
private fun LogDialog(
    entries: List<LogEntry>,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    val c = iosColors()
    val clipboard = LocalClipboardManager.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = c.bg) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Журнал",
                        color = c.textPrimary, fontSize = 17.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(AppLogger.export()))
                    }) { Text("Копир.", color = c.accent) }
                    TextButton(onClick = onSave) { Text("TXT", color = c.accent) }
                    TextButton(onClick = onClose) { Text("Закрыть", color = c.accent) }
                }
                HorizontalDivider(color = c.separator.copy(alpha = 0.5f))
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Пока пусто", color = c.textSecondary)
                    }
                } else {
                    val reversed = entries.asReversed()
                    LazyColumn(Modifier.fillMaxSize().weight(1f).padding(horizontal = 8.dp)) {
                        items(reversed) { e ->
                            val color = when (e.level) {
                                LogLevel.DEBUG -> c.textSecondary
                                LogLevel.INFO -> c.textPrimary
                                LogLevel.WARN -> Color(0xFFFF9500)
                                LogLevel.ERROR -> c.red
                            }
                            Text(
                                e.formatted(),
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = c.separator.copy(alpha = 0.5f))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Записей: ${entries.size}", color = c.textSecondary, fontSize = 12.sp)
                    TextButton(onClick = onClear) { Text("Очистить", color = c.red) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val c = iosColors()
    Text(
        text.uppercase(),
        color = c.textSecondary, fontSize = 13.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun UpdateDialog(
    savedUrl: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = iosColors()
    var url by remember { mutableStateOf(savedUrl) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val isDirty = url.trim() != savedUrl.trim()

    fun validateAndSave() {
        val t = url.trim()
        if (t.isEmpty()) { onSave(""); return }
        if (!t.startsWith("https://") && !t.startsWith("http://")) {
            errorText = "Ссылка должна начинаться с https:// или http://"
            return
        }
        if (!t.endsWith(".apk", ignoreCase = true)) {
            errorText = "Ссылка должна заканчиваться на .apk"
            return
        }
        errorText = null
        onSave(t)
    }

    fun tryDismiss() {
        if (isDirty) showDiscardConfirm = true else onDismiss()
    }

    IosAlertDialog(
        title = "Обновление",
        message = "Вставьте ссылку на APK (обычно с GitHub Releases).",
        confirmLabel = "Сохранить",
        dismissLabel = "Отмена",
        onConfirm = { validateAndSave() },
        onDismiss = { tryDismiss() }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; errorText = null },
                placeholder = { Text("https://github.com/.../app.apk") },
                singleLine = true,
                isError = errorText != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            if (errorText != null) {
                Spacer(Modifier.height(6.dp))
                Text(errorText!!, color = c.red, fontSize = 12.sp)
            }
            if (savedUrl.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Сохранено:", color = c.textSecondary, fontSize = 12.sp)
                        Text(
                            savedUrl,
                            color = c.textPrimary, fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { onSave("") }) {
                        Text("Удалить", color = c.red, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showDiscardConfirm) {
        IosAlertDialog(
            title = "Несохранённые изменения",
            message = "В поле есть ссылка, которая не сохранена. Стереть её?",
            confirmLabel = "Стереть",
            dismissLabel = "Отмена",
            destructive = true,
            onConfirm = {
                showDiscardConfirm = false
                onDismiss()
            },
            onDismiss = { showDiscardConfirm = false }
        )
    }
}