package com.rgblight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.data.EffectCatalog
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.theme.iosColors

@Composable
fun EffectsScreen(vm: MainViewModel) {
    val c = iosColors()
    val currentEffect by vm.currentEffectId.collectAsState()
    val speed by vm.effectSpeed.collectAsState()
    val brightness by vm.effectBrightness.collectAsState()
    val savedEffects by vm.savedEffects.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<EffectCatalog.Category?>(null) }
    var saveDialogFor by remember { mutableStateOf<EffectCatalog.Effect?>(null) }

    val filtered = remember(query, selectedCat) {
        val base = if (selectedCat == null) EffectCatalog.effects else EffectCatalog.byCategory(selectedCat)
        if (query.isBlank()) base else base.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {

        // Поиск
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Поиск эффекта…", color = c.textSecondary) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = c.textSecondary) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.separator,
                focusedTextColor = c.textPrimary,
                unfocusedTextColor = c.textPrimary
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Чипы категорий
        LazyRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCat == null,
                    onClick = { selectedCat = null },
                    label = { Text("Все") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.accent.copy(alpha = 0.2f),
                        selectedLabelColor = c.accent
                    )
                )
            }
            items(EffectCatalog.categories) { cat ->
                FilterChip(
                    selected = selectedCat == cat,
                    onClick = { selectedCat = cat },
                    label = { Text(cat.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.accent.copy(alpha = 0.2f),
                        selectedLabelColor = c.accent
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Кнопка Стоп (если играет эффект)
        if (currentEffect != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { vm.stopEffect() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Стоп эффект")
                }
            }
        }

        // Список
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(filtered, key = { it.id }) { effect ->
                val isPlaying = currentEffect == effect.id
                val isSaved = savedEffects.any { it.effectId == effect.id }
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isPlaying) c.accent.copy(alpha = 0.15f) else c.surface, RoundedCornerShape(10.dp))
                        .clickable { vm.startEffect(effect.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(32.dp).background(
                            if (isPlaying) c.accent else c.surfaceSecondary,
                            RoundedCornerShape(8.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${effect.id}",
                            color = if (isPlaying) Color.White else c.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(effect.name, color = c.textPrimary, fontSize = 16.sp)
                        Text(effect.cat.label, color = c.textSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = {
                        if (isSaved) {
                            savedEffects.firstOrNull { it.effectId == effect.id }
                                ?.let { vm.deleteSavedEffect(it.id) }
                        } else {
                            saveDialogFor = effect
                        }
                    }) {
                        Icon(
                            if (isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                            null,
                            tint = if (isSaved) Color(0xFFFFCC00) else c.textSecondary
                        )
                    }
                    Icon(
                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        null,
                        tint = if (isPlaying) c.red else c.accent
                    )
                }
            }
        }

        // Ползунки скорость + яркость внизу
        Surface(color = c.surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Скорость  $speed", color = c.textSecondary, fontSize = 12.sp)
                    Slider(
                        value = speed.toFloat(),
                        onValueChange = { vm.setEffectSpeed(it.toInt()) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = c.accent,
                            inactiveTrackColor = c.separator.copy(alpha = 0.4f)
                        )
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Яркость  $brightness%", color = c.textSecondary, fontSize = 12.sp)
                    Slider(
                        value = brightness.toFloat(),
                        onValueChange = { vm.setEffectBrightness(it.toInt()) },
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
    }

    if (saveDialogFor != null) {
        SaveNameDialog(
            title = "Сохранить эффект",
            defaultName = saveDialogFor!!.name,
            onSave = { name ->
                vm.saveEffectWithCurrentState(saveDialogFor!!.id, name)
                saveDialogFor = null
            },
            onDismiss = { saveDialogFor = null }
        )
    }
}

@Composable
internal fun SaveNameDialog(
    title: String,
    defaultName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = iosColors()
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = c.textPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(defaultName) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.separator,
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }) {
                Text("Сохранить", color = c.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = c.accent) }
        },
        containerColor = c.surface
    )
}