package com.rgblight.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rgblight.data.ColorSlot
import com.rgblight.ui.theme.iosColors

/**
 * Ряд слотов сохранённых цветов.
 * Используем Row + horizontalScroll, а не LazyRow — потому что этот ряд
 * встраивается в вертикально-скроллируемую колонку (ColorScreen).
 */
@Composable
fun ColorSlotRow(
    slots: List<ColorSlot>,
    activeIndex: Int?,
    onActivate: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = iosColors()
    val activeCount = slots.count { it.rgb != null }

    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { idx, slot ->
            if (slot.rgb != null) {
                SlotCircle(
                    rgb = slot.rgb,
                    isActive = activeIndex == idx,
                    onClick = { onActivate(idx) },
                    onLongClick = { onLongPress(idx) }
                )
            } else if (idx == activeCount) {
                // Первый пустой после заполненных — кнопка «+»
                Box(
                    Modifier.size(40.dp)
                        .background(c.surfaceSecondary, RoundedCornerShape(50))
                        .border(1.dp, c.separator, RoundedCornerShape(50))
                        .clickable { onAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = c.accent, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun SlotCircle(
    rgb: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val c = iosColors()
    val infinite = rememberInfiniteTransition(label = "slotPulse")
    val alpha by if (isActive) {
        infinite.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulse"
        )
    } else remember { mutableStateOf(0f) }

    Box(
        Modifier
            .size(40.dp)
            .then(
                if (isActive)
                    Modifier.border(3.dp, c.accent.copy(alpha = alpha), RoundedCornerShape(50))
                else Modifier.border(1.dp, c.separator, RoundedCornerShape(50))
            )
            .padding(3.dp)
            .background(Color(rgb), RoundedCornerShape(50))
            .pointerInput(rgb) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    )
}