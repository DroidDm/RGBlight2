package com.rgblight.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    modifier: Modifier = Modifier,
    onChange: (hue: Float, sat: Float) -> Unit
) {
    val rainbow = listOf(
        Color.Red, Color.Yellow, Color.Green,
        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )

    Box(modifier.fillMaxWidth().padding(8.dp).aspectRatio(1f)) {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos -> handle(pos, size.width.toFloat(), size.height.toFloat(), onChange) },
                        onDrag = { change, _ -> handle(change.position, size.width.toFloat(), size.height.toFloat(), onChange) }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { pos -> handle(pos, size.width.toFloat(), size.height.toFloat(), onChange) }
                }
        ) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.sweepGradient(rainbow, center),
                radius = r * 0.98f,
                center = center,
                style = Stroke(width = r * 0.06f)
            )

            val hueColor = Color(
                AndroidColor.HSVToColor(floatArrayOf(((hue % 360f) + 360f) % 360f, 1f, 1f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, hueColor),
                    center = center,
                    radius = r * 0.94f
                ),
                radius = r * 0.94f,
                center = center
            )

            val angleRad = Math.toRadians(hue.toDouble())
            val dist = saturation.coerceIn(0f, 1f) * r * 0.94f
            val px = center.x + (cos(angleRad) * dist).toFloat()
            val py = center.y + (sin(angleRad) * dist).toFloat()

            drawCircle(color = Color.White, radius = r * 0.05f, center = Offset(px, py), style = Stroke(width = 3f))
            drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = r * 0.05f, center = Offset(px, py), style = Stroke(width = 1.5f))
        }
    }
}

private fun handle(pos: Offset, w: Float, h: Float, onChange: (Float, Float) -> Unit) {
    val center = Offset(w / 2f, h / 2f)
    val dx = pos.x - center.x
    val dy = pos.y - center.y
    val dist = sqrt(dx * dx + dy * dy)
    val maxR = w / 2f * 0.94f
    val sat = (dist / maxR).coerceIn(0f, 1f)
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0) angle += 360f
    onChange(angle, sat)
}