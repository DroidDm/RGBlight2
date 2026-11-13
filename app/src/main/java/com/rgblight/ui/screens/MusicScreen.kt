package com.rgblight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rgblight.ui.MainViewModel
import com.rgblight.ui.MusicSource
import com.rgblight.ui.components.IosSegmentedControl
import com.rgblight.ui.components.PreviewBox
import com.rgblight.ui.theme.iosColors

@Composable
fun MusicScreen(vm: MainViewModel) {
    val c = iosColors()
    val source by vm.musicSource.collectAsState()
    val playing by vm.isMusicPlaying.collectAsState()
    val level by vm.micLevel.collectAsState()

    Column(
        Modifier.fillMaxSize().background(c.bg).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PreviewBox(Color(0xFFFF2D55).copy(alpha = 0.4f + level * 0.6f), height = 80)
        Spacer(Modifier.height(20.dp))

        Text("Источник музыки", color = c.textPrimary, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        IosSegmentedControl(
            options = listOf("Микрофон ленты", "Микрофон телефона"),
            selectedIndex = if (source == MusicSource.CONTROLLER) 0 else 1,
            onSelect = { vm.setMusicSource(if (it == 0) MusicSource.CONTROLLER else MusicSource.PHONE) }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            when (source) {
                MusicSource.CONTROLLER -> "Лента слушает свой микрофон. Телефон отправляет только команду «вкл/выкл»."
                MusicSource.PHONE -> "Телефон слушает микрофон и меняет цвет/яркость ленты в такт звуку."
            },
            color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        if (source == MusicSource.PHONE) {
            Text("Уровень микрофона: ${(level * 100).toInt()}%",
                color = c.textPrimary, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { level.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = c.accent,
                trackColor = c.separator.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.weight(1f))

        if (playing) {
            Button(
                onClick = { vm.stopMusic() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.red)
            ) {
                Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp))
                Text("Остановить", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = { vm.startMusic() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent)
            ) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp))
                Text("Включить цветомузыку", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}