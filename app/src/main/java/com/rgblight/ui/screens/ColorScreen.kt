package com.rgblight.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.components.*
import com.rgblight.ui.theme.iosColors

private val QUICK_COLORS = listOf(
    0xFFFFFF, 0xFFE4B5, 0xFF0000, 0xFF9500, 0xFFFF00,
    0x00FF00, 0x00FFFF, 0x0000FF, 0xAF52DE, 0xFF2D55
)

@Composable
fun ColorScreen(vm: MainViewModel) {
    val c = iosColors()
    val state by vm.colorState.collectAsState()
    val settings by vm.settings.collectAsState()
    val slots by vm.colorSlots.collectAsState()
    val active by vm.activeSlot.collectAsState()
    var showSave by remember { mutableStateOf(false) }
    var deleteSlot by remember { mutableStateOf<Int?>(null) }

    // Текущий RGB — вычисляется из HSV-состояния (state.value = яркость 0..100)
    val rgb = AndroidColor.HSVToColor(
        floatArrayOf(state.hue, state.saturation, state.value / 100f)
    )

    Column(
        Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // Превью
        PreviewBox(Color(rgb), height = 90)
        Spacer(Modifier.height(16.dp))

        // Круг справа + RGB слева
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RgbColumn(
                r = (rgb shr 16) and 0xFF,
                g = (rgb shr 8) and 0xFF,
                b = rgb and 0xFF,
                onChange = { r, g, b -> vm.updateRgb(r, g, b) },
                modifier = Modifier.weight(0.42f)
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(0.58f)) {
                ColorWheel(
                    hue = state.hue,
                    saturation = state.saturation,
                    onChange = { h, s -> vm.updateColor(h, s) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Быстрые цвета
        Text("Быстрые цвета", color = c.textPrimary, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QUICK_COLORS) { argb ->
                Box(
                    Modifier.size(40.dp)
                        .background(Color(argb), RoundedCornerShape(50))
                        .clickable { vm.applyPreset(argb) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Сохранённые слоты
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Мои цвета", color = c.textPrimary, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (active != null) {
                Text("активный: ${active!! + 1}",
                    color = c.accent, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        ColorSlotRow(
            slots = slots,
            activeIndex = active,
            onActivate = { vm.activateSlot(it) },
            onLongPress = { deleteSlot = it },
            onAdd = { vm.addEmptySlot() }
        )

        Spacer(Modifier.height(20.dp))

        // Ползунки
        DualSliders(
            speed = settings.speed,
            brightness = settings.brightness,
            onSpeed = { vm.setSpeed(it) },
            onBrightness = { vm.updateBrightness(it.toFloat()) }
        )
        Spacer(Modifier.height(8.dp))
        HoldRampSliders(
            holdMs = settings.holdMs,
            rampMs = settings.rampMs,
            onHold = { vm.setHoldMs(it) },
            onRamp = { vm.setRampMs(it) }
        )

        Spacer(Modifier.height(20.dp))

        // Сохранить
        Button(
            onClick = { showSave = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent)
        ) {
            Icon(Icons.Default.Star, null)
            Spacer(Modifier.width(8.dp))
            Text("Сохранить цвет", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showSave) {
        IosInputDialog(
            title = "Сохранить цвет",
            initial = "#%02X%02X%02X".format((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF),
            placeholder = "Имя",
            onSave = { vm.saveCurrentColor(it); showSave = false },
            onDismiss = { showSave = false }
        )
    }
    if (deleteSlot != null) {
        IosAlertDialog(
            title = "Удалить слот?",
            message = "Цвет из этого слота будет удалён.",
            confirmLabel = "Удалить",
            dismissLabel = "Отмена",
            destructive = true,
            onConfirm = { vm.clearSlot(deleteSlot!!); deleteSlot = null },
            onDismiss = { deleteSlot = null }
        )
    }
}