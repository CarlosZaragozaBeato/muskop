package com.muskop.android.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.android.library.ResourceLearningControl
import com.muskop.android.library.organizationLabel
import com.muskop.session.PracticeEntryInput
import com.muskop.session.ResourceLearningUpdate
import com.muskop.session.SessionDocument
import com.muskop.session.practiceReview

@Composable
internal fun PracticeSummaryScreen(
    session: SessionDocument,
    input: PracticeEntryInput,
    padding: PaddingValues,
    onSavePractice: (PracticeEntryInput, (Result<Unit>) -> Unit) -> Unit,
    onSaveLearning: (String, ResourceLearningUpdate, (Result<Unit>) -> Unit) -> Unit,
    onDone: () -> Unit,
    onReturnToPractice: () -> Unit,
) {
    val review = remember(session, input.id) { input.id?.let(session::practiceReview) }
    var attempt by rememberSaveable(input.id) { mutableStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var savingResource by remember { mutableStateOf<String?>(null) }
    var assessmentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(input.id, attempt) {
        if (review == null) {
            saveError = null
            onSavePractice(input) { result ->
                result.onFailure { saveError = it.message ?: "The practice could not be saved." }
            }
        }
    }
    // The history is the success signal, including when a save finishes during recreation.
    BackHandler {
        if (review != null && savingResource == null) onDone()
        else if (review == null && saveError != null) onReturnToPractice()
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Practice summary", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
                Text(input.routineName ?: if (input.routineId == "free") "Free practice" else "Routine practice",
                    style = MaterialTheme.typography.titleLarge)
            }
        }
        if (review == null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (saveError == null) {
                        CircularProgressIndicator()
                        Text("Saving your practice…")
                    } else {
                        Text("Practice has not been saved.", style = MaterialTheme.typography.titleMedium)
                        Text(saveError.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Text("Your review is kept here. Retry to save it once.")
                        Button(onClick = { saveError = null; attempt += 1 }) { Text("Retry save") }
                        androidx.compose.material3.TextButton(onClick = onReturnToPractice) { Text("Return to practice") }
                    }
                }
            }
        } else {
            item {
                SummaryCard {
                    Text("Practice saved", style = MaterialTheme.typography.titleMedium)
                    Text(summaryDuration(review.entry.seconds.toLong()), style = MaterialTheme.typography.displaySmall)
                    Text(when {
                        review.entry.routineId == "free" -> "Free practice finished"
                        review.entry.completed -> "Routine completed"
                        else -> "Partial routine saved"
                    })
                    Text("${review.resources.size} resources practiced")
                    if (review.unassignedSeconds > 0) Text("${summaryDuration(review.unassignedSeconds)} without a linked resource",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SummaryCard {
                    Text("Day overview · ${review.day.date}", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                    Text(summaryDuration(review.day.seconds), style = MaterialTheme.typography.headlineMedium)
                    Text("${review.day.practiceCount} practices saved · ${review.day.resourceCount} resources practiced")
                    Text("${review.day.completedRoutineCount} routines completed")
                    Text("Includes this practice. Time is grouped by the day each practice was saved.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Practiced resources", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                    Text("Review your current learning status and mastery. Practice time does not change either automatically.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    assessmentMessage?.let { Text(it) }
                    if (review.resources.isEmpty()) Text("No resource was linked while the timer was running. Your full practice time is saved.")
                }
            }
            items(review.resources, key = { it.id }) { practiced ->
                SummaryCard {
                    val resource = practiced.resource
                    Text(resource?.title ?: "Resource unavailable", style = MaterialTheme.typography.titleLarge)
                    Text("This practice · ${summaryDuration(practiced.seconds.toLong())}")
                    Text("All saved practice · ${summaryDuration(practiced.history.totalSeconds)} · ${practiced.history.practiceCount} practices",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (resource != null) {
                        resource.organizationLabel()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        ResourceLearningControl(resource, onSave = { update ->
                            savingResource = resource.id
                            assessmentMessage = null
                            onSaveLearning(resource.id, update) { result ->
                                savingResource = null
                                assessmentMessage = result.fold(
                                    onSuccess = { "Assessment saved · ${resource.title}" },
                                    onFailure = { "Assessment not saved · ${resource.title}: ${it.message ?: "Please try again."}" },
                                )
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = savingResource == null)
                        if (savingResource == resource.id) Text("Saving assessment…")
                    } else Text("Its recorded time is kept. An assessment needs a resource with a unique id in Library.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Button(onClick = onDone, enabled = savingResource == null, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

private fun summaryDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val remaining = seconds % 60
    return if (hours > 0) "${hours}h ${minutes}m ${remaining}s" else "${minutes}m ${remaining}s"
}
