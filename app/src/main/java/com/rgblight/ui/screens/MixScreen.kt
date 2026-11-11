package com.rgblight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.rgblight.data.MixMode
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.components.ColorWheel
import com.rgblight.ui.components.IosSegmentedControl
import com.rgblight.ui.theme.iosColors

@Composable
fun MixScreen(vm: MainViewModel) {
    val c = iosColors()
    val colors by vm.mixColors.collectAsState()
    val mode by vm.mixMode.collectAsState()
    val speed by vm.mixSpeed.collectAsState()
    val brightness by vm.mixBrightness.collectAsState()
    val isPlaying by vm.isMixPlaying.collectAsState()
    val colorState by vm.colorState.collectAsState()

    var showAddColor by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.bg)) {

        Surface(color = c.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Цвета микса (${colors.size}/12)",
                        color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (colors.isNotEmpty()) {
                        TextButton(onClick = { vm.clearMix() }) {
                            Icon(Icons.Default.Delete, null, tint = c.red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Очистить", color = c.red, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(colors) { index, colorInt ->
                        Box(
                            Modifier.size(44.dp).background(Color(colorInt), RoundedCornerShape(50))
                                .border(2.dp, c.separator, RoundedCornerShape(50))
                        ) {
                            Box(
                                Modifier.align(Alignment.TopEnd).size(20.dp)
                                    .background(c.surface, RoundedCornerShape(50))
                                    .clickable { vm.removeMixColor(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                "${index + 1}",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                            )
                        }
                    }
                    if (colors.size < 12) {
                        item {
                            Box(
                                Modifier.size(44.dp)
                                    .background(c.surfaceSecondary, RoundedCornerShape(50))
                                    .border(1.dp, c.separator, RoundedCornerShape(50))
                                    .clickable { showAddColor = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = c.accent)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Режим", color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            IosSegmentedControl(
                options = MixMode.values().map { it.label },
                selectedIndex = MixMode.values().indexOf(mode),
                onSelect = { vm.setMixMode(MixMode.values()[it]) }
            )
        }

        Spacer(Modifier.height(16.dp))

        Surface(color = c.surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Скорость  $speed", color = c.textSecondary, fontSize = 12.sp)
                    Slider(
                        value = speed.toFloat(),
                        onValueChange = { vm.setMixSpeed(it.toInt()) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = c.accent,
                            inactiveTrackColor = c.separator.copy(alpha = 0.4f)
                        )
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Яркость  $brightness%", color = c.textSecondary, fontSize = 12.sp)
                    Slider(
                        value = brightness.toFloat(),
                        onValueChange = { vm.setMixBrightness(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = c.accent,
                            inactiveTrackColor = c.separator.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isPlaying) {
                Button(
                    onClick = { vm.startMix() },
                    enabled = colors.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Запустить", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { vm.stopMix() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.red)
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Стоп", fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(
                onClick = { showSaveDialog = true },
                enabled = colors.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = c.accent)
            }
        }
    }

    if (showAddColor) {
        AddColorDialog(
            initialHue = colorState.hue,
            initialSat = colorState.saturation,
            onPick = { rgb -> vm.addMixColor(rgb); showAddColor = false },
            onDismiss = { showAddColor = false }
        )
    }

    if (showSaveDialog) {
        SaveNameDialog(
            title = "Сохранить микс",
            defaultName = "Микс ${colors.size} цветов",
            onSave = { name -> vm.saveCurrentMix(name); showSaveDialog = false },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@Composable
private fun AddColorDialog(
    initialHue: Float,
    initialSat: Float,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = iosColors()
    var hue by remember { mutableFloatStateOf(initialHue) }
    var sat by remember { mutableFloatStateOf(initialSat) }
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите цвет", color = c.textPrimary) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ColorWheel(
                    hue = hue,
                    saturation = sat,
                    onChange = { h, s -> hue = h; sat = s }
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.size(48.dp).background(Color(rgb), RoundedCornerShape(10.dp))
                        .border(1.dp, c.separator, RoundedCornerShape(10.dp))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(rgb) }) {
                Text("Добавить", color = c.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = c.accent) } },
        containerColor = c.surface
    )
}