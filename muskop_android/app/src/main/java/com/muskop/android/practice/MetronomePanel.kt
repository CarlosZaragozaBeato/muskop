package com.muskop.android.practice

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MetronomePanel(initialBpm: Int? = null) {
    var bpm by rememberSaveable { mutableIntStateOf(initialBpm ?: 80) }
    var enabled by rememberSaveable { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }

    DisposableEffect(tone) { onDispose { tone.release() } }
    LaunchedEffect(initialBpm) {
        if (initialBpm != null) bpm = initialBpm.coerceIn(20, 300)
    }
    LaunchedEffect(enabled, bpm) {
        beat = 0
        while (enabled) {
            beat = (beat % 4) + 1
            tone.startTone(
                if (beat == 1) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP,
                60,
            )
            delay((60_000L / bpm).coerceAtLeast(1L))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Metronome", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (enabled) "$bpm BPM · beat $beat" else "Choose a tempo for your practice",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = { enabled = !enabled }) {
                    Icon(
                        if (enabled) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (enabled) "Stop metronome" else "Start metronome",
                    )
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf(60, 80, 100, 120, 140) + listOfNotNull(initialBpm)).distinct().sorted().forEach { preset ->
                    FilterChip(
                        selected = bpm == preset,
                        onClick = { bpm = preset },
                        label = {
                            Text(if (preset == initialBpm) "$preset · Routine" else "$preset")
                        },
                    )
                }
            }
        }
    }
}
