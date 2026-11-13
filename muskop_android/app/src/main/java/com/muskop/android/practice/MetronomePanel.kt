package com.muskop.android.practice

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MetronomePanel(initialBpm: Int? = null) {
    var bpm by rememberSaveable { mutableIntStateOf(initialBpm ?: 80) }
    var enabled by rememberSaveable { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }
    var showTempos by rememberSaveable { mutableStateOf(false) }
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.GraphicEq, contentDescription = null)
        TextButton(
            onClick = { showTempos = true },
            modifier = Modifier.weight(1f),
        ) {
            Text(
                "Metronome · $bpm BPM · ${if (enabled) "On" else "Off"}",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        IconButton(onClick = { enabled = !enabled }) {
            Icon(
                if (enabled) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (enabled) "Stop metronome" else "Start metronome",
                tint = if (enabled && beat == 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (showTempos) {
        AlertDialog(
            onDismissRequest = { showTempos = false },
            title = { Text("Metronome tempo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$bpm BPM")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (listOf(60, 80, 100, 120, 140) + listOfNotNull(initialBpm))
                            .distinct().sorted().forEach { preset ->
                                FilterChip(
                                    selected = bpm == preset,
                                    onClick = { bpm = preset },
                                    label = { Text(if (preset == initialBpm) "$preset · Routine" else "$preset") },
                                )
                            }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTempos = false }) { Text("Done") }
            },
        )
    }
}
