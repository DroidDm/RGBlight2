package com.rgblight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.ui.theme.iosColors

/** Левая колонка: три поля RGB + превью. Тап на поле = цифровая клавиатура + −/+. */
@Composable
fun RgbColumn(
    r: Int, g: Int, b: Int,
    onChange: (r: Int, g: Int, b: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = iosColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RgbField(label = "R", value = r, tint = Color(0xFFFF3B30)) { onChange(it, g, b) }
        RgbField(label = "G", value = g, tint = Color(0xFF34C759)) { onChange(r, it, b) }
        RgbField(label = "B", value = b, tint = Color(0xFF007AFF)) { onChange(r, g, it) }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(56.dp)
                .background(Color(r, g, b), RoundedCornerShape(10.dp))
        )
        Text(
            "#%02X%02X%02X".format(r, g, b),
            color = c.textSecondary, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RgbField(
    label: String, value: Int, tint: Color, onChange: (Int) -> Unit
) {
    val c = iosColors()
    var showDialog by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .background(c.surface, RoundedCornerShape(8.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(18.dp))
        Text(
            value.toString(),
            color = c.textPrimary, fontSize = 16.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End
        )
    }
    if (showDialog) {
        RgbInputDialog(label, value, onChange) { showDialog = false }
    }
}

@Composable
private fun RgbInputDialog(
    label: String, value: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit
) {
    val c = iosColors()
    var text by remember { mutableStateOf(value.toString()) }

    fun apply(v: String) {
        val n = v.toIntOrNull()?.coerceIn(0, 255) ?: return
        onChange(n); onDismiss()
    }

    IosAlertDialog(
        title = "Введите $label (0..255)",
        confirmLabel = "Готово",
        dismissLabel = "Отмена",
        onConfirm = { apply(text) },
        onDismiss = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(c.surfaceSecondary, RoundedCornerShape(8.dp))
                        .clickable {
                            val n = (text.toIntOrNull() ?: 0) - 1
                            text = n.coerceIn(0, 255).toString()
                        },
                    contentAlignment = Alignment.Center
                ) { Text("−", fontSize = 22.sp, color = c.textPrimary) }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(3) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center, fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(44.dp).background(c.surfaceSecondary, RoundedCornerShape(8.dp))
                        .clickable {
                            val n = (text.toIntOrNull() ?: 0) + 1
                            text = n.coerceIn(0, 255).toString()
                        },
                    contentAlignment = Alignment.Center
                ) { Text("+", fontSize = 22.sp, color = c.textPrimary) }
            }
        }
    }
}