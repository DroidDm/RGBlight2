package com.rgblight.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rgblight.data.EffectMode

/** Статичное превью: квадрат цвета. */
@Composable
fun PreviewBox(color: Color, modifier: Modifier = Modifier, height: Int = 80) {
    Box(modifier.fillMaxWidth().height(height.dp).background(color, RoundedCornerShape(12.dp)))
}

/** Анимированное превью: переливание между двумя цветами (оставлено для совместимости). */
@Composable
fun PreviewPulse(
    colorA: Color, colorB: Color, modifier: Modifier = Modifier, height: Int = 80
) {
    val transition = rememberInfiniteTransition(label = "preview")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "t"
    )
    val mixed = Color(
        red = colorA.red * (1f - t) + colorB.red * t,
        green = colorA.green * (1f - t) + colorB.green * t,
        blue = colorA.blue * (1f - t) + colorB.blue * t
    )
    Box(modifier.fillMaxWidth().height(height.dp).background(mixed, RoundedCornerShape(12.dp)))
}

/**
 * Умное превью выбранного эффекта:
 *  - isProtocol = true → анимация по protocolId (0..7)
 *  - isProtocol = false → анимация по mode и текущим colors
 */
@Composable
fun EffectPreview(
    isProtocol: Boolean,
    protocolId: Int?,
    mode: EffectMode,
    colors: List<Int>,
    modifier: Modifier = Modifier,
    height: Int = 80
) {
    if (isProtocol) ProtocolPreview(protocolId, modifier, height)
    else CustomPreview(mode, colors, modifier, height)
}

@Composable
private fun ProtocolPreview(id: Int?, modifier: Modifier, height: Int) {
    val pid = id ?: 0
    val t by rememberInfiniteTransition(label = "protoPreview").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "protoT"
    )
    val color: Color = when (pid) {
        // 0 Статика — сплошной белый
        0 -> Color(0xFFFFFFFF)
        // 1 Прыжок — резкая смена RGB
        1 -> listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF))[(t * 3).toInt() % 3]
        // 2 Плавный переход — радуга
        2 -> composeHsv(t * 360f, 1f, 1f)
        // 3 Вспышка — пульсирующий красный
        3 -> {
            val k = (1f - (t * 2 % 1f)).coerceIn(0.15f, 1f)
            Color(k, 0f, 0f)
        }
        // 4 Дыхание — пульсирующий голубой
        4 -> {
            val k = 0.3f + 0.7f * (1f - (t * 2 % 1f))
            Color(0f, 0.55f * k, 1f * k)
        }
        // 5 Радуга
        5 -> composeHsv(t * 360f, 1f, 1f)
        // 6 Музыка — радуга с пульсацией яркости
        6 -> composeHsv(t * 360f, 0.9f, 0.5f + 0.5f * (1f - (t * 3 % 1f)))
        // 7 Волна — пастельная радуга
        7 -> composeHsv(t * 360f, 0.55f, 1f)
        else -> Color(0xFF888888)
    }
    Box(modifier.fillMaxWidth().height(height.dp).background(color, RoundedCornerShape(12.dp)))
}

@Composable
private fun CustomPreview(mode: EffectMode, colors: List<Int>, modifier: Modifier, height: Int) {
    val c1 = colors.firstOrNull()?.let { Color(it) } ?: Color(0xFFFF0000)
    val c2 = colors.getOrNull(1)?.let { Color(it) } ?: c1
    val c3 = colors.getOrNull(2)?.let { Color(it) } ?: c1

    val t by rememberInfiniteTransition(label = "customPreview").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "customT"
    )
    val color = when (mode) {
        EffectMode.FADE, EffectMode.FADE_PAUSE, EffectMode.FADE_CUSTOM -> mix(c1, c2, t)
        EffectMode.FADE_PULSE -> mix(c1, mix(c1, Color.Black, 0.5f), t)
        EffectMode.PULSE -> mix(Color.Black, c1, t)
        EffectMode.JUMP -> listOf(c1, c2, c3)[(t * 3).toInt() % 3]
        EffectMode.NEW_YEAR -> composeHsv(t * 360f, 1f, 1f)
    }
    Box(modifier.fillMaxWidth().height(height.dp).background(color, RoundedCornerShape(12.dp)))
}

private fun mix(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * k,
        green = a.green + (b.green - a.green) * k,
        blue = a.blue + (b.blue - a.blue) * k,
        alpha = 1f
    )
}

private fun composeHsv(h: Float, s: Float, v: Float): Color {
    val norm = ((h % 360f) + 360f) % 360f
    val i = AndroidColor.HSVToColor(
        floatArrayOf(norm, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    )
    return Color(i)
}