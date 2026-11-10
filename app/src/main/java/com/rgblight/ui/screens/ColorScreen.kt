package com.rgblight.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rgblight.ui.components.ColorWheel
import com.rgblight.ui.theme.iosColors

@Composable
fun ColorScreen(vm: MainViewModel) {
    val c = iosColors()
    val state by vm.colorState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(c.bg).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        ColorWheel(
            hue = state.hue,
            saturation = state.saturation,
            onChange = { h, s -> vm.updateColor(h, s) }
        )

        Spacer(Modifier.height(8.dp))

        val rgbPreview = AndroidColor.HSVToColor(
            floatArrayOf(state.hue, state.saturation, state.value / 100f)
        )
        val r = rgbPreview shr 16 and 0xFF
        val g = rgbPreview shr 8 and 0xFF
        val b = rgbPreview and 0xFF

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(56.dp)
                    .background(Color(rgbPreview), RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "#%06X".format(0xFFFFFF and rgbPreview),
                    color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                )
                Text("R$r  G$g  B$b", color = c.textSecondary, fontSize = 13.sp)
            }
            IconButton(onClick = { showSaveDialog = true }) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFCC00))
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Яркость", color = c.textPrimary, fontSize = 15.sp)
                Text("${state.value.toInt()}%", color = c.textSecondary, fontSize = 15.sp)
            }
            Slider(
                value = state.value,
                onValueChange = { vm.updateBrightness(it) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = c.accent,
                    inactiveTrackColor = c.separator.copy(alpha = 0.4f)
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        Text("Быстрые цвета", color = c.textPrimary, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                Color.White, Color(0xFFFFE4B5), Color.Red, Color(0xFFFF9500),
                Color.Yellow, Color.Green, Color.Cyan, Color.Blue,
                Color(0xFFAF52DE), Color(0xFFFF2D55)
            ).forEach { col ->
                Box(
                    Modifier.size(28.dp)
                        .background(col, RoundedCornerShape(50))
                        .clickable {
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(col.value.toLong().toInt(), hsv)
                            vm.updateColor(hsv[0], hsv[1])
                        }
                )
            }
        }

        Spacer(Modifier.weight(1f))
    }

    if (showSaveDialog) {
        SaveNameDialog(
            title = "Сохранить цвет",
            defaultName = "#%06X".format(
                0xFFFFFF and AndroidColor.HSVToColor(
                    floatArrayOf(state.hue, state.saturation, 1f)
                )
            ),
            onSave = { name -> vm.saveCurrentColor(name); showSaveDialog = false },
            onDismiss = { showSaveDialog = false }
        )
    }
}