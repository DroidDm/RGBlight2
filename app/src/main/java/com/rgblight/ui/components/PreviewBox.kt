package com.rgblight.ui.components

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

/** Статичное превью: квадрат цвета. */
@Composable
fun PreviewBox(color: Color, modifier: Modifier = Modifier, height: Int = 80) {
    Box(modifier.fillMaxWidth().height(height.dp).background(color, RoundedCornerShape(12.dp)))
}

/** Анимированное превью эффекта: переливается между двумя цветами. */
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