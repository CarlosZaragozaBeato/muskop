package com.muskop.android.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.session.ActivePracticeSnapshot
import com.muskop.session.PracticeRoutine
import com.muskop.session.SessionResource
import com.muskop.session.totalMinutes

@Composable
internal fun RoutinePreparationScreen(
    routine: PracticeRoutine,
    resources: Map<String, SessionResource>,
    resume: ActivePracticeSnapshot?,
    padding: PaddingValues,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val completed = resume?.let {
        it.secondsLeft == 0 && it.blockIndex >= routine.blocks.lastIndex
    } == true

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to routines")
                }
                Text("Routine", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit routine")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                routine.category?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(routine.name, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
                Text("${routine.blocks.size} blocks · ${routine.totalMinutes()} min",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                routine.description?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        when {
                            routine.blocks.isEmpty() -> "Add a block to get started"
                            completed -> "Your practice is ready to save"
                            resume != null -> "Continue from block ${(resume.blockIndex + 1).coerceAtMost(routine.blocks.size)}"
                            else -> "Ready when you are"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        when {
                            routine.blocks.isEmpty() -> "Give your routine a practice block and a duration."
                            completed -> "Open your completed practice to review and save it."
                            resume != null -> "Your saved time will be restored when you continue."
                            else -> "Review your blocks below. The timer starts when you tap Start practice."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = if (routine.blocks.isEmpty()) onEdit else onStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (routine.blocks.isEmpty()) Icons.Rounded.Edit else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(when {
                            routine.blocks.isEmpty() -> "Add blocks"
                            completed -> "Review practice"
                            resume != null -> "Continue practice"
                            else -> "Start practice"
                        })
                    }
                }
            }
        }
        if (routine.blocks.isNotEmpty()) {
            item {
                Text("Practice blocks", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
            }
        }
        itemsIndexed(routine.blocks) { index, block ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Block ${index + 1}", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(block.name, style = MaterialTheme.typography.titleMedium)
                    Text("${block.minutes} min · ${block.bpm?.let { "$it BPM" } ?: "Free tempo"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    block.resourceId?.let { id ->
                        Text(resources[id]?.title ?: "Linked resource unavailable",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    block.notes?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
