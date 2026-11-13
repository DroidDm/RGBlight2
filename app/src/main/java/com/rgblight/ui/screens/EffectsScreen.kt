package com.rgblight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.data.EffectCatalog
import com.rgblight.data.EffectMode
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.components.*
import com.rgblight.ui.theme.iosColors

@Composable
fun EffectsScreen(vm: MainViewModel) {
    val c = iosColors()
    val settings by vm.settings.collectAsState()
    val mode by vm.effectMode.collectAsState()
    val colors by vm.effectColors.collectAsState()
    val protocolId by vm.protocolId.collectAsState()
    val playing by vm.isEffectPlaying.collectAsState()

    var seg by remember { mutableIntStateOf(0) }  // 0 = protocol, 1 = custom
    var showSave by remember { mutableStateOf(false) }
    var showAddColor by remember { mutableStateOf(false) }
    var pendingColor by remember { mutableIntStateOf(0xFF0000) }

    Column(
        Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // Анимированное превью
        val previewA = colors.firstOrNull() ?: 0xFF0000
        val previewB = colors.getOrNull(1) ?: previewA
        PreviewPulse(Color(previewA), Color(previewB), height = 80)
        Spacer(Modifier.height(16.dp))

        IosSegmentedControl(
            options = listOf("Протокольные", "Приложение"),
            selectedIndex = seg,
            onSelect = { seg = it; vm.setProtocolId(null) }
        )

        Spacer(Modifier.height(16.dp))

        if (seg == 0) {
            // === Протокольные ===
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EffectCatalog.protocolEffects.forEach { (id, name) ->
                    val selected = protocolId == id
                    Row(
                        Modifier.fillMaxWidth()
                            .background(
                                if (selected) c.accent.copy(alpha = 0.15f) else c.surface,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { vm.setProtocolId(id); vm.playEffect() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#$id", color = c.textSecondary, fontSize = 12.sp,
                            modifier = Modifier.width(36.dp))
                        Text(name, color = c.textPrimary, fontSize = 15.sp,
                            modifier = Modifier.weight(1f))
                        if (selected && playing)
                            Text("▶", color = c.accent, fontSize = 14.sp)
                    }
                }
            }
        } else {
            // === Кастомные (7 режимов) ===
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(EffectCatalog.modes) { _, meta ->
                    FilterChip(
                        selected = mode == meta.mode,
                        onClick = { vm.setEffectMode(meta.mode) },
                        label = { Text(meta.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = c.accent.copy(alpha = 0.2f),
                            selectedLabelColor = c.accent
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            val meta = EffectCatalog.modes.first { it.mode == mode }
            if (meta.needColors) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Цвета", color = c.textPrimary, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (colors.size < 8) IconButton(onClick = { pendingColor = colors.firstOrNull() ?: 0xFF0000; showAddColor = true }) {
                        Icon(Icons.Default.Add, null, tint = c.accent)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(colors) { idx, argb ->
                        Box(
                            Modifier.size(40.dp)
                                .background(Color(argb), RoundedCornerShape(50))
                                .clickable {
                                    if (colors.size > 1) vm.removeEffectColor(idx)
                                }
                        ) {
                            if (colors.size > 1) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).size(16.dp)
                                        .background(c.surface, RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null,
                                        tint = c.textSecondary, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        DualSliders(
            speed = settings.speed, brightness = settings.brightness,
            onSpeed = { vm.setSpeed(it) },
            onBrightness = { vm.setGlobalBrightness(it) }
        )
        Spacer(Modifier.height(8.dp))
        HoldRampSliders(
            holdMs = settings.holdMs, rampMs = settings.rampMs,
            onHold = { vm.setHoldMs(it) }, onRamp = { vm.setRampMs(it) }
        )

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (playing) {
                Button(
                    onClick = { vm.stopEffect() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.red)
                ) {
                    Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp))
                    Text("Стоп", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { vm.playEffect() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent)
                ) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp))
                    Text("Запустить", fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(
                onClick = { showSave = true },
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = c.accent)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showSave) {
        IosInputDialog(
            title = "Сохранить эффект",
            initial = "",
            placeholder = "Имя эффекта",
            onSave = { vm.saveCurrentEffect(it); showSave = false },
            onDismiss = { showSave = false }
        )
    }
    if (showAddColor) {
        AddColorPickerDialog(
            initial = pendingColor,
            onPick = { vm.addEffectColor(it); showAddColor = false },
            onDismiss = { showAddColor = false }
        )
    }
}

@Composable
fun AddColorPickerDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = iosColors()
    var rgb by remember { mutableIntStateOf(initial) }
    val hsv = remember { FloatArray(3).also { AndroidColorHSV(rgb, it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    val computed = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f))
    rgb = computed

    IosAlertDialog(
        title = "Выбрать цвет",
        confirmLabel = "Добавить",
        dismissLabel = "Отмена",
        onConfirm = { onPick(rgb) },
        onDismiss = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            ColorWheel(hue = hue, saturation = sat, onChange = { h, s -> hue = h; sat = s })
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(48.dp).background(Color(rgb), RoundedCornerShape(10.dp)))
        }
    }
}

private fun AndroidColorHSV(rgb: Int, out: FloatArray) {
    android.graphics.Color.colorToHSV(rgb, out)
}