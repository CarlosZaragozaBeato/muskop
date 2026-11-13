package com.muskop.android.library

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muskop.android.practice.PausePracticeOnBackground

/** Reading scroll is independent of metronome BPM, playback and practice timers. */
@Composable
internal fun TabReadingSheet(
    title: String,
    onClose: () -> Unit,
    score: LazyListScope.(showDetails: Boolean, zoom: Float) -> Unit,
) {
    val list = rememberLazyListState()
    var scrolling by remember { mutableStateOf(false) }
    var speed by rememberSaveable { mutableFloatStateOf(16f) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val dragging by list.interactionSource.collectIsDraggedAsState()
    val pixelsPerSecond = with(LocalDensity.current) { speed.dp.toPx() }

    PausePracticeOnBackground { scrolling = false }
    LaunchedEffect(dragging) { if (dragging) scrolling = false }
    LaunchedEffect(scrolling, pixelsPerSecond) {
        if (!scrolling) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (scrolling) {
            val now = withFrameNanos { it }
            val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(0.1f)
            previous = now
            if (!list.canScrollForward) {
                scrolling = false
            } else {
                list.scrollBy(pixelsPerSecond * seconds)
            }
        }
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val previous = view.keepScreenOn
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = previous }
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { scrolling = false; showDetails = !showDetails }) {
                        Icon(Icons.Rounded.Info, contentDescription = if (showDetails) "Hide score details" else "Show score details")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close full score")
                    }
                }
                LazyColumn(
                    state = list,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) { score(showDetails, zoom) }
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scrolling = !scrolling }, enabled = scrolling || list.canScrollForward) {
                                Icon(if (scrolling) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (scrolling) "Pause reading scroll" else "Start reading scroll")
                            }
                            Text(if (scrolling) "Scrolling" else "Reading scroll",
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { scrolling = false; zoom = (zoom - 0.25f).coerceAtLeast(0.75f) }, enabled = zoom > 0.75f) {
                                Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out score")
                            }
                            Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { scrolling = false; zoom = (zoom + 0.25f).coerceAtMost(2f) }, enabled = zoom < 2f) {
                                Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in score")
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(8f to "Slow", 16f to "Medium", 24f to "Fast").forEach { (value, label) ->
                                FilterChip(selected = speed == value, onClick = { speed = value }, label = { Text(label) })
                            }
                        }
                    }
                }
            }
        }
    }
}
