package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.muskop.session.ResourceOrganizationUpdate
import com.muskop.session.ResourcePriority
import com.muskop.session.ResourceStatus
import com.muskop.session.ResourceView
import com.muskop.session.SessionResource
import com.muskop.session.organizationStatus
import com.muskop.session.resourcePriority

internal fun ResourceStatus.organizationLabel(): String = when (this) {
    ResourceStatus.ACTIVE -> "Active"; ResourceStatus.HIDDEN -> "Hidden"; ResourceStatus.COMPLETED -> "Completed"
}
internal fun ResourcePriority.priorityLabel(): String = when (this) {
    ResourcePriority.HIGH -> "High priority"; ResourcePriority.NORMAL -> "Normal priority"; ResourcePriority.LOW -> "Low priority"
}
internal fun SessionResource.organizationLabel(): String? = listOfNotNull(
    organizationStatus()?.takeUnless { it == ResourceStatus.ACTIVE }?.organizationLabel(),
    resourcePriority()?.takeUnless { it == ResourcePriority.NORMAL }?.priorityLabel(),
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

@Composable
internal fun ResourceViewPicker(view: ResourceView, onChange: (ResourceView) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ResourceView.entries, key = { it.name }) { option ->
            FilterChip(selected = view == option, onClick = { onChange(option) }, label = { Text(when (option) {
                ResourceView.ACTIVE -> "Active"; ResourceView.HIDDEN -> "Hidden"; ResourceView.COMPLETED -> "Completed"; ResourceView.ALL -> "All resources"
            }) })
        }
    }
}

@Composable
internal fun ResourceOrganizationControl(resource: SessionResource, onSave: (ResourceOrganizationUpdate) -> Unit) {
    val currentStatus = resource.organizationStatus()
    val currentPriority = resource.resourcePriority()
    var open by rememberSaveable(resource.id) { mutableStateOf(false) }
    var status by rememberSaveable(resource.id) { mutableStateOf<ResourceStatus?>(null) }
    var priority by rememberSaveable(resource.id) { mutableStateOf<ResourcePriority?>(null) }
    TextButton(onClick = { status = currentStatus; priority = currentPriority; open = true },
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
        Text("Organize · ${currentStatus?.organizationLabel() ?: "Status unavailable"} · ${currentPriority?.priorityLabel() ?: "Priority unavailable"}")
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false }, title = { Text("Organize resource") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hidden and completed resources stay in your session. Only active resources appear by default when choosing what to practice.")
                Text("Visibility", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    ResourceStatus.entries.forEach { option ->
                        OrganizationChoice(option.organizationLabel(), status == option) { status = option }
                    }
                }
                Text("Priority", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    ResourcePriority.entries.forEach { option ->
                        OrganizationChoice(option.priorityLabel(), priority == option) { priority = option }
                    }
                }
                Text("Priority sorts resource lists. Existing routine blocks keep their order.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(enabled = status != currentStatus || priority != currentPriority, onClick = {
            onSave(ResourceOrganizationUpdate(status.takeIf { it != currentStatus }, priority.takeIf { it != currentPriority })); open = false
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
    )
}

@Composable
private fun OrganizationChoice(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onSelect)
        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected, onClick = null)
        Text(label, Modifier.weight(1f))
    }
}
