package com.rgblight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.BuildConfig
import com.rgblight.data.MixBackgroundMode
import com.rgblight.data.ThemeMode
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.components.*
import com.rgblight.ui.theme.iosColors

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val c = iosColors()
    val settings by vm.settings.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }

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
                Spacer(Modifier.height(8.dp))
                Text(
                    when (settings.mixBackground) {
                        MixBackgroundMode.ASK -> "При выходе будет спрос"
                        MixBackgroundMode.ALWAYS -> "Микс продолжит работать в уведомлении"
                        MixBackgroundMode.NEVER -> "Микс остановится при выходе"
                    },
                    color = c.textSecondary, fontSize = 13.sp
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