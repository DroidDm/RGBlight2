package com.rgblight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.ui.theme.iosColors

/** Два ползунка в одной строке: «Скорость» и «Яркость». Значения в % справа. */
@Composable
fun DualSliders(
    speed: Int, brightness: Int,
    onSpeed: (Int) -> Unit,
    onBrightness: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = iosColors()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Скорость", color = c.textSecondary, fontSize = 12.sp)
                Text("${(speed / 255f * 100).toInt()}%", color = c.textSecondary, fontSize = 12.sp)
            }
            Slider(
                value = speed.toFloat(),
                onValueChange = { onSpeed(it.toInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = c.accent,
                    inactiveTrackColor = c.separator.copy(alpha = 0.4f)
                )
            )
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Яркость", color = c.textSecondary, fontSize = 12.sp)
                Text("$brightness%", color = c.textSecondary, fontSize = 12.sp)
            }
            Slider(
                value = brightness.toFloat(),
                onValueChange = { onBrightness(it.toInt()) },
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

/** Дополнительная строка: «Удержание» (сек) и «Розжиг» (мс). */
@Composable
fun HoldRampSliders(
    holdMs: Int, rampMs: Int,
    onHold: (Int) -> Unit,
    onRamp: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = iosColors()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Горит", color = c.textSecondary, fontSize = 12.sp)
                Text("%.1f с".format(holdMs / 1000f), color = c.textSecondary, fontSize = 12.sp)
            }
            Slider(
                value = holdMs.toFloat(),
                onValueChange = { onHold(it.toInt()) },
                valueRange = 200f..10000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = c.accent,
                    inactiveTrackColor = c.separator.copy(alpha = 0.4f)
                )
            )
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Розжиг", color = c.textSecondary, fontSize = 12.sp)
                Text("${rampMs} мс", color = c.textSecondary, fontSize = 12.sp)
            }
            Slider(
                value = rampMs.toFloat(),
                onValueChange = { onRamp(it.toInt()) },
                valueRange = 100f..3000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = c.accent,
                    inactiveTrackColor = c.separator.copy(alpha = 0.4f)
                )
            )
        }
    }
}