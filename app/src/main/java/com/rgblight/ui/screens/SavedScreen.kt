package com.rgblight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.data.SavedColor
import com.rgblight.data.SavedEffect
import com.rgblight.data.SavedMix
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.theme.iosColors

@Composable
fun SavedScreen(vm: MainViewModel) {
    val c = iosColors()
    val colors by vm.savedColors.collectAsState()
    val effects by vm.savedEffects.collectAsState()
    val mixes by vm.savedMixes.collectAsState()

    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        // Переключатель
        Box(Modifier.padding(16.dp)) {
            com.rgblight.ui.components.IosSegmentedControl(
                options = listOf("Цвета (${colors.size})", "Эффекты (${effects.size})", "Миксы (${mixes.size})"),
                selectedIndex = tab,
                onSelect = { tab = it }
            )
        }

        if ((tab == 0 && colors.isEmpty()) || (tab == 1 && effects.isEmpty()) || (tab == 2 && mixes.isEmpty())) {
            EmptyState(c)
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                when (tab) {
                    0 -> items(colors, key = { it.id }) { color ->
                        SavedColorRow(
                            color = color,
                            onApply = { vm.applySavedColor(color) },
                            onDelete = { vm.deleteSavedColor(color.id) }
                        )
                    }
                    1 -> items(effects, key = { it.id }) { effect ->
                        SavedEffectRow(
                            effect = effect,
                            onApply = { vm.applySavedEffect(effect) },
                            onDelete = { vm.deleteSavedEffect(effect.id) }
                        )
                    }
                    2 -> items(mixes, key = { it.id }) { mix ->
                        SavedMixRow(
                            mix = mix,
                            onApply = { vm.applySavedMix(mix) },
                            onDelete = { vm.deleteSavedMix(mix.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(c: com.rgblight.ui.theme.IosColors) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⭐", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Здесь пока пусто",
            color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Сохраняйте цвета, эффекты и миксы кнопкой ⭐",
            color = c.textSecondary, fontSize = 14.sp
        )
    }
}

@Composable
private fun SavedColorRow(
    color: SavedColor,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val c = iosColors()
    Row(
        Modifier.fillMaxWidth()
            .background(c.surface, RoundedCornerShape(10.dp))
            .clickable { onApply() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(
                Color(color.r, color.g, color.b), RoundedCornerShape(50)
            )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(color.name, color = c.textPrimary, fontSize = 16.sp)
            Text(
                "#%02X%02X%02X".format(color.r, color.g, color.b),
                color = c.textSecondary, fontSize = 12.sp
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = c.red)
        }
        Icon(Icons.Default.PlayArrow, null, tint = c.accent)
    }
}

@Composable
private fun SavedEffectRow(
    effect: SavedEffect,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val c = iosColors()
    Row(
        Modifier.fillMaxWidth()
            .background(c.surface, RoundedCornerShape(10.dp))
            .clickable { onApply() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(c.accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("#${effect.effectId}", color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(effect.name, color = c.textPrimary, fontSize = 16.sp)
            Text(
                "Скорость ${effect.speed} · Яркость ${effect.brightness}%",
                color = c.textSecondary, fontSize = 12.sp
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = c.red)
        }
        Icon(Icons.Default.PlayArrow, null, tint = c.accent)
    }
}

@Composable
private fun SavedMixRow(
    mix: SavedMix,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val c = iosColors()
    Row(
        Modifier.fillMaxWidth()
            .background(c.surface, RoundedCornerShape(10.dp))
            .clickable { onApply() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.width(44.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            mix.colors.take(4).forEach { col ->
                Box(
                    Modifier.size(9.dp).background(Color(col), RoundedCornerShape(50))
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(mix.name, color = c.textPrimary, fontSize = 16.sp)
            Text(
                "${mix.colors.size} цветов · ${mix.mode.label} · ${mix.speed}",
                color = c.textSecondary, fontSize = 12.sp
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = c.red)
        }
        Icon(Icons.Default.PlayArrow, null, tint = c.accent)
    }
}