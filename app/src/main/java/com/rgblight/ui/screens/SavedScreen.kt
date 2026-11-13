package com.rgblight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.data.SavedColor
import com.rgblight.data.SavedEffect
import com.rgblight.data.SavedMix
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.components.*
import com.rgblight.ui.theme.iosColors

@Composable
fun SavedScreen(vm: MainViewModel) {
    val c = iosColors()
    val colors by vm.savedColors.collectAsState()
    val effects by vm.savedEffects.collectAsState()
    val mixes by vm.savedMixes.collectAsState()

    var seg by remember { mutableIntStateOf(0) }
    var deleteColor by remember { mutableStateOf<SavedColor?>(null) }
    var deleteEffect by remember { mutableStateOf<SavedEffect?>(null) }
    var deleteMix by remember { mutableStateOf<SavedMix?>(null) }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.padding(16.dp)) {
            IosSegmentedControl(
                options = listOf("Цвета (${colors.size})", "Эффекты (${effects.size})", "Миксы (${mixes.size})"),
                selectedIndex = seg,
                onSelect = { seg = it }
            )
        }
        when (seg) {
            0 -> if (colors.isEmpty()) EmptyBlock("Цветов пока нет") else
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(colors, key = { it.id }) { col ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(c.surface, RoundedCornerShape(10.dp))
                                .pointerInput(col.id) {
                                    detectTapGestures(
                                        onTap = { vm.applySavedColor(col) },
                                        onLongPress = { deleteColor = col }
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(40.dp).background(Color(col.r, col.g, col.b), RoundedCornerShape(50)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(col.name, color = c.textPrimary, fontSize = 16.sp)
                                Text("#%02X%02X%02X".format(col.r, col.g, col.b),
                                    color = c.textSecondary, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.PlayArrow, null, tint = c.accent)
                        }
                    }
                }
            1 -> if (effects.isEmpty()) EmptyBlock("Эффектов пока нет") else
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(effects, key = { it.id }) { eff ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(c.surface, RoundedCornerShape(10.dp))
                                .pointerInput(eff.id) {
                                    detectTapGestures(
                                        onTap = { vm.applySavedEffect(eff) },
                                        onLongPress = { deleteEffect = eff }
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tag = eff.protocolId?.let { "#$it" } ?: eff.mode.name.take(3)
                            Box(Modifier.size(40.dp).background(c.accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center) {
                                Text(tag, color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(eff.name, color = c.textPrimary, fontSize = 16.sp)
                                Text("${eff.colors.size} цв · %.1f с · ${eff.speed}".
                                    format(eff.holdMs / 1000f), color = c.textSecondary, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.PlayArrow, null, tint = c.accent)
                        }
                    }
                }
            2 -> if (mixes.isEmpty()) EmptyBlock("Миксов пока нет") else
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(mixes, key = { it.id }) { mix ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(c.surface, RoundedCornerShape(10.dp))
                                .pointerInput(mix.id) {
                                    detectTapGestures(
                                        onTap = { vm.applySavedMix(mix) },
                                        onLongPress = { deleteMix = mix }
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(Modifier.width(60.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(mix.steps.take(4)) { step ->
                                    val col = if (step.color != 0) Color(step.color)
                                              else Color(step.colors.firstOrNull() ?: 0xFF0000)
                                    Box(Modifier.size(12.dp).background(col, RoundedCornerShape(50)))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mix.name, color = c.textPrimary, fontSize = 16.sp)
                                Text("${mix.steps.size} шагов · ${mix.speed}",
                                    color = c.textSecondary, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.PlayArrow, null, tint = c.accent)
                        }
                    }
                }
        }
    }

    deleteColor?.let {
        IosAlertDialog(
            title = "Удалить цвет?",
            message = "«${it.name}» будет удалён.",
            destructive = true, confirmLabel = "Удалить", dismissLabel = "Отмена",
            onConfirm = { vm.deleteSavedColor(it.id); deleteColor = null },
            onDismiss = { deleteColor = null }
        )
    }
    deleteEffect?.let {
        IosAlertDialog(
            title = "Удалить эффект?",
            message = "«${it.name}» будет удалён.",
            destructive = true, confirmLabel = "Удалить", dismissLabel = "Отмена",
            onConfirm = { vm.deleteSavedEffect(it.id); deleteEffect = null },
            onDismiss = { deleteEffect = null }
        )
    }
    deleteMix?.let {
        IosAlertDialog(
            title = "Удалить микс?",
            message = "«${it.name}» будет удалён.",
            destructive = true, confirmLabel = "Удалить", dismissLabel = "Отмена",
            onConfirm = { vm.deleteSavedMix(it.id); deleteMix = null },
            onDismiss = { deleteMix = null }
        )
    }
}

@Composable
private fun EmptyBlock(text: String) {
    val c = iosColors()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("⭐", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(text, color = c.textSecondary, fontSize = 15.sp)
    }
}