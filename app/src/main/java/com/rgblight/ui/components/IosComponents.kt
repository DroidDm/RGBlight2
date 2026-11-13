package com.rgblight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rgblight.ui.theme.iosColors

@Composable
fun IosTopBar(
    title: String,
    subtitle: String? = null,
    statusColor: Color = Color(0xFF8E8E93),
    rightActions: @Composable RowScope.() -> Unit = {}
) {
    val c = iosColors()
    Surface(color = c.bg, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(statusColor, RoundedCornerShape(50)))
                        Spacer(Modifier.width(6.dp))
                        Text(subtitle, color = c.textSecondary, fontSize = 13.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = rightActions)
        }
    }
}

@Composable
fun IosTabBar(
    items: List<Triple<String, ImageVector, Boolean>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val c = iosColors()
    Surface(color = c.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, (label, icon, enabled) ->
                val isSel = i == selected
                val tint = when {
                    !enabled -> c.textSecondary.copy(alpha = 0.4f)
                    isSel -> c.accent
                    else -> c.textSecondary
                }
                Column(
                    Modifier.clickable(enabled) { onSelect(i) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
                    Text(label, color = tint, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun IosCard(content: @Composable ColumnScope.() -> Unit) {
    val c = iosColors()
    Column(
        Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        content = content
    )
}

@Composable
fun IosRow(
    icon: ImageVector? = null,
    iconTint: Color? = null,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val c = iosColors()
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .heightIn(min = 44.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = iconTint ?: c.accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = c.textSecondary, fontSize = 13.sp)
        }
        trailing()
    }
}

@Composable
fun IosDivider() {
    val c = iosColors()
    HorizontalDivider(color = c.separator.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@Composable
fun IosSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = iosColors()
    Row(
        modifier.background(c.surfaceSecondary.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
            .padding(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(
                Modifier.weight(1f)
                    .background(if (sel) c.surface else Color.Transparent, RoundedCornerShape(7.dp))
                    .clickable { onSelect(i) }.padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 13.sp, color = c.textPrimary,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun IosAlertDialog(
    title: String,
    message: String? = null,
    confirmLabel: String = "ОК",
    dismissLabel: String? = "Отмена",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val c = iosColors()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(14.dp), color = c.surface, modifier = Modifier.width(270.dp)) {
            Column(Modifier.padding(top = 20.dp)) {
                Text(title, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                if (message != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(message, color = c.textPrimary, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }
                content()
                Spacer(Modifier.height(16.dp))
                IosDivider()
                Row(Modifier.fillMaxWidth().height(44.dp)) {
                    if (dismissLabel != null) {
                        Box(Modifier.weight(1f).fillMaxHeight().clickable { onDismiss() },
                            contentAlignment = Alignment.Center) {
                            Text(dismissLabel, color = c.accent, fontSize = 17.sp)
                        }
                        Box(Modifier.width(0.5.dp).fillMaxHeight().background(c.separator))
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onConfirm() },
                        contentAlignment = Alignment.Center) {
                        Text(confirmLabel, color = if (destructive) c.red else c.accent,
                            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Простое окно ввода текста, iOS-стиль. */
@Composable
fun IosInputDialog(
    title: String,
    initial: String = "",
    placeholder: String = "",
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = iosColors()
    var value by remember { androidx.compose.runtime.mutableStateOf(initial) }
    IosAlertDialog(
        title = title,
        confirmLabel = "Сохранить",
        dismissLabel = "Отмена",
        onConfirm = { onSave(value.trim()) },
        onDismiss = onDismiss
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder, color = c.textSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}