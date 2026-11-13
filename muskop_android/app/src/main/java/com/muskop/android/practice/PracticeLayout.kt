package com.muskop.android.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun PracticeLayout(
    padding: PaddingValues,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    controls: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Leave and continue later")
            }
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() })
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content(Modifier.weight(1f).fillMaxWidth())
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) { controls() }
        }
    }
}

@Composable
internal fun PracticeControls(
    timer: String,
    status: String,
    running: Boolean,
    primaryLabel: String,
    onToggle: () -> Unit,
    onFinish: () -> Unit,
    finishEnabled: Boolean,
    finished: Boolean = false,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    previousEnabled: Boolean = false,
    nextDescription: String = "Next block",
) {
    Text("$timer · $status", style = MaterialTheme.typography.titleMedium)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onPrevious != null && !finished) {
            IconButton(onClick = onPrevious, enabled = previousEnabled) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous block")
            }
        }
        Button(onClick = if (finished) onFinish else onToggle, modifier = Modifier.weight(1f)) {
            if (!finished) {
                Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
            }
            Text(primaryLabel)
        }
        if (onNext != null && !finished) {
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = nextDescription)
            }
        }
    }
    if (!finished) {
        TextButton(onClick = onFinish, enabled = finishEnabled, modifier = Modifier.fillMaxWidth()) {
            Text("Finish practice")
        }
    }
}
