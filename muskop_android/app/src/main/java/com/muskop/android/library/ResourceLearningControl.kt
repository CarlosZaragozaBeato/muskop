package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.session.ResourceLearningStatus
import com.muskop.session.ResourceLearningUpdate
import com.muskop.session.SessionResource
import com.muskop.session.learningStatus
import com.muskop.session.masteryPercent
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.math.roundToInt

internal fun ResourceLearningStatus.displayLabel(): String = when (this) {
    ResourceLearningStatus.NOT_STARTED -> "Not started"
    ResourceLearningStatus.LEARNING -> "Learning"
    ResourceLearningStatus.PLAYABLE -> "Playable"
    ResourceLearningStatus.MAINTAINING -> "Maintaining"
}

internal fun SessionResource.learningSummaryLabel(): String? = listOfNotNull(
    learningStatus()?.displayLabel(), masteryPercent()?.let { "Mastery $it%" },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

@Composable
internal fun ResourceLearningControl(
    resource: SessionResource,
    onSave: (ResourceLearningUpdate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val current = resource.learningStatus()
    val currentMastery = resource.masteryPercent()
    val rawLearning = resource.learning
    val hasUnsupportedStatus = current == null && rawLearning != null && rawLearning !is JsonNull &&
        (rawLearning !is JsonObject || rawLearning["status"]?.let { it !is JsonNull } == true)
    var showDialog by rememberSaveable(resource.id) { mutableStateOf(false) }
    var draft by rememberSaveable(resource.id) { mutableStateOf<ResourceLearningStatus?>(null) }
    var draftMastery by rememberSaveable(resource.id) { mutableStateOf<Int?>(null) }
    var statusEdited by rememberSaveable(resource.id) { mutableStateOf(false) }
    var masteryEdited by rememberSaveable(resource.id) { mutableStateOf(false) }
    val rawMastery = (rawLearning as? JsonObject)?.get("masteryPercent")
    val hasUnsupportedMastery = currentMastery == null && rawMastery != null && rawMastery !is JsonNull
    val updateStatus = draft != current || (hasUnsupportedStatus && statusEdited)
    val updateMastery = draftMastery != currentMastery || (hasUnsupportedMastery && masteryEdited)
    TextButton(
        enabled = enabled,
        onClick = {
            draft = current; draftMastery = currentMastery
            statusEdited = false; masteryEdited = false; showDialog = true
        },
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Text("Learning status · ${resource.learningSummaryLabel() ?: if (hasUnsupportedStatus) "Unavailable" else "Not assessed"}")
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Learning status") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your own assessment of this resource. Practice time and difficulty do not change it automatically.")
                    if (hasUnsupportedStatus) Text("The saved status is not supported. Choose a status to replace it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.selectableGroup()) {
                        (listOf<ResourceLearningStatus?>(null) + ResourceLearningStatus.entries).forEach { status ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .selectable(selected = draft == status, role = Role.RadioButton, onClick = { draft = status; statusEdited = true })
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(selected = draft == status, onClick = null)
                                Text(status?.displayLabel() ?: "Not assessed", Modifier.weight(1f))
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                            value = draftMastery != null, role = Role.Switch,
                            onValueChange = { enabled -> draftMastery = if (enabled) currentMastery ?: 0 else null; masteryEdited = true },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Assess mastery percentage", Modifier.weight(1f))
                        Switch(checked = draftMastery != null, onCheckedChange = null)
                    }
                    Text(draftMastery?.let { "Mastery: $it%" } ?: "Mastery not assessed",
                        style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = (draftMastery ?: 0).toFloat(),
                        onValueChange = { draftMastery = it.roundToInt(); masteryEdited = true },
                        valueRange = 0f..100f, steps = 99, enabled = draftMastery != null,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Mastery percentage" },
                    )
                    Text("Your estimate from 0% to 100%. It does not change your learning status automatically.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (hasUnsupportedMastery) Text("The saved percentage is not supported. Enable an assessment to replace it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(ResourceLearningUpdate(draft, draftMastery, updateStatus, updateMastery)); showDialog = false
                }, enabled = updateStatus || updateMastery) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        )
    }
}
