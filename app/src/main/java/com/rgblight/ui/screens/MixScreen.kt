package com.rgblight.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.rgblight.data.EffectMode
import com.rgblight.data.StepKind
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.MixUiStep
import com.rgblight.ui.components.*
import com.rgblight.ui.theme.iosColors

@Composable
fun MixScreen(vm: MainViewModel) {
    val c = iosColors()
    val steps by vm.mixSteps.collectAsState()
    val settings by vm.settings.collectAsState()
    val playing by vm.isMixPlaying.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var showSave by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.bg).padding(16.dp)) {
        // Статичное превью первого шага
        val preview = steps.firstOrNull()?.let { step ->
            if (step.kind == StepKind.COLOR) Color(step.color) else Color(step.colors.firstOrNull() ?: 0xFF0000)
        } ?: Color(0xFFEEEEEE)
        PreviewBox(preview, height = 60)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Шаги микса (${steps.size}/20)",
                color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            if (steps.isNotEmpty()) IconButton(onClick = { vm.clearMix() }) {
                Icon(Icons.Default.Delete, null, tint = c.red)
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(steps) { idx, step ->
                MixStepCard(
                    index = idx, step = step,
                    onClick = { editIndex = idx }
                )
            }
            if (steps.size < 20) {
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(c.surface, RoundedCornerShape(10.dp))
                            .clickable { showAdd = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = c.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить шаг", color = c.accent, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        DualSliders(
            speed = settings.speed, brightness = settings.brightness,
            onSpeed = { vm.setSpeed(it) },
            onBrightness = { vm.setGlobalBrightness(it) }
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (playing) {
                Button(
                    onClick = { vm.stopMix() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.red)
                ) {
                    Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp))
                    Text("Стоп", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { vm.playMix() },
                    enabled = steps.isNotEmpty(),
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
                enabled = steps.isNotEmpty(),
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) { Icon(Icons.Default.Star, null, tint = c.accent) }
        }
    }

    if (showAdd) {
        AddStepDialog(
            onPick = { kind ->
                val step = if (kind == StepKind.COLOR) {
                    val rgb = AndroidColor.HSVToColor(floatArrayOf(0f, 0f, 1f))
                    MixUiStep(kind = StepKind.COLOR, color = rgb)
                } else {
                    MixUiStep(kind = StepKind.EFFECT, effectMode = EffectMode.FADE)
                }
                vm.addMixStep(step)
                showAdd = false
                editIndex = vm.mixSteps.value.size - 1
            },
            onDismiss = { showAdd = false }
        )
    }
    if (editIndex >= 0 && editIndex < steps.size) {
        EditStepDialog(
            step = steps[editIndex],
            onSave = { vm.updateMixStep(editIndex, it); editIndex = -1 },
            onDelete = { vm.removeMixStep(editIndex); editIndex = -1 },
            onDismiss = { editIndex = -1 }
        )
    }
    if (showSave) {
        IosInputDialog(
            title = "Сохранить микс",
            initial = "",
            placeholder = "Имя микса",
            onSave = { vm.saveCurrentMix(it); showSave = false },
            onDismiss = { showSave = false }
        )
    }
}

@Composable
private fun MixStepCard(index: Int, step: MixUiStep, onClick: () -> Unit) {
    val c = iosColors()
    Row(
        Modifier.fillMaxWidth()
            .background(c.surface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${index + 1}", color = c.textSecondary, fontSize = 12.sp,
            modifier = Modifier.width(24.dp))
        if (step.kind == StepKind.COLOR) {
            Box(Modifier.size(36.dp).background(Color(step.color), RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Цвет", color = c.textPrimary, fontSize = 14.sp)
                Text("#%06X · %.1f с".format(step.color and 0xFFFFFF, step.holdMs / 1000f),
                    color = c.textSecondary, fontSize = 12.sp)
            }
        } else {
            LazyRow(Modifier.width(70.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                items(step.colors.take(4)) { argb ->
                    Box(Modifier.size(14.dp).background(Color(argb), RoundedCornerShape(50)))
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(step.effectMode.name, color = c.textPrimary, fontSize = 14.sp)
                Text("${step.colors.size} цветов · %.1f с".format(step.holdMs / 1000f),
                    color = c.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AddStepDialog(onPick: (StepKind) -> Unit, onDismiss: () -> Unit) {
    IosAlertDialog(
        title = "Добавить шаг",
        confirmLabel = "",
        dismissLabel = "Отмена",
        onConfirm = { onDismiss() },
        onDismiss = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onPick(StepKind.COLOR) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Цвет") }
            OutlinedButton(
                onClick = { onPick(StepKind.EFFECT) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Эффект") }
        }
    }
}

@Composable
private fun EditStepDialog(
    step: MixUiStep,
    onSave: (MixUiStep) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = iosColors()
    var holdMs by remember { mutableIntStateOf(step.holdMs) }
    var rampMs by remember { mutableIntStateOf(step.rampMs) }
    var color by remember { mutableIntStateOf(step.color) }
    var effectMode by remember { mutableStateOf(step.effectMode) }
    var colors by remember { mutableStateOf(step.colors) }

    IosAlertDialog(
        title = if (step.kind == StepKind.COLOR) "Цвет шага" else "Эффект шага",
        confirmLabel = "Готово",
        dismissLabel = "Отмена",
        onConfirm = {
            onSave(
                step.copy(
                    color = color, effectMode = effectMode,
                    colors = colors, holdMs = holdMs, rampMs = rampMs
                )
            )
        },
        onDismiss = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {

            if (step.kind == StepKind.COLOR) {
                Text("Цвет", color = c.textPrimary, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(
                        0xFFFFFF, 0xFF0000, 0x00FF00, 0x0000FF,
                        0xFFFF00, 0xFF00FF, 0x00FFFF, 0xFF9500
                    )) { argb ->
                        Box(
                            Modifier.size(30.dp)
                                .background(Color(argb), RoundedCornerShape(50))
                                .clickable { color = argb }
                        )
                    }
                }
            } else {
                Text("Режим", color = c.textPrimary, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EffectMode.values().toList()) { m ->
                        FilterChip(
                            selected = effectMode == m,
                            onClick = { effectMode = m },
                            label = { Text(m.name, fontSize = 11.sp) }
                        )
                    }
                }
                Text("Цвета (${colors.size})", color = c.textPrimary, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(colors) { idx, argb ->
                        Box(
                            Modifier.size(30.dp)
                                .background(Color(argb), RoundedCornerShape(50))
                                .clickable {
                                    if (colors.size > 1)
                                        colors = colors.toMutableList().also { it.removeAt(idx) }
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Горит: %.1f с".format(holdMs / 1000f), color = c.textSecondary, fontSize = 12.sp)
            Slider(
                value = holdMs.toFloat(),
                onValueChange = { holdMs = it.toInt() },
                valueRange = 200f..10000f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = c.accent)
            )
            Text("Розжиг: ${rampMs} мс", color = c.textSecondary, fontSize = 12.sp)
            Slider(
                value = rampMs.toFloat(),
                onValueChange = { rampMs = it.toInt() },
                valueRange = 100f..3000f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = c.accent)
            )

            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null, tint = c.red)
                Spacer(Modifier.width(6.dp))
                Text("Удалить шаг", color = c.red)
            }
        }
    }
}