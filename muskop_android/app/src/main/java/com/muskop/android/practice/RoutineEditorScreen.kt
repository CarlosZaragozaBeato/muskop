package com.muskop.android.practice

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.session.PracticeRoutine
import com.muskop.session.RoutineBlockInput
import com.muskop.session.RoutineEditorInput
import com.muskop.session.SessionResource

@Composable
fun RoutineEditorScreen(
    routine: PracticeRoutine?,
    resources: List<SessionResource>,
    padding: PaddingValues,
    onSave: (RoutineEditorInput) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember(routine) { mutableStateOf(routine?.name.orEmpty()) }
    var description by remember(routine) { mutableStateOf(routine?.description.orEmpty()) }
    var category by remember(routine) { mutableStateOf(routine?.category.orEmpty()) }
    var showDetails by rememberSaveable(routine?.id) { mutableStateOf(description.isNotBlank() || category.isNotBlank()) }
    var blocks by remember(routine) {
        mutableStateOf(
            routine?.blocks?.map { block ->
                RoutineBlockInput(block.id, block.name, block.minutes, block.bpm, block.resourceId, block.skill, block.notes)
            } ?: listOf(newBlock()),
        )
    }

    fun update(index: Int, block: RoutineBlockInput) {
        blocks = blocks.toMutableList().apply { set(index, block) }
    }

    fun move(index: Int, offset: Int) {
        val target = index + offset
        if (target !in blocks.indices) return
        blocks = blocks.toMutableList().apply { add(target, removeAt(index)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("← Back to routines") } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (routine == null) "New routine" else "Edit routine",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Build your practice with simple, focused blocks.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Routine name") },
                        singleLine = true,
                    )
                    TextButton(onClick = { showDetails = !showDetails }) {
                        Icon(
                            if (showDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                        )
                        Text(if (showDetails) "Hide details" else "Add description and category")
                    }
                    if (showDetails) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Optional category") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Optional description") },
                            minLines = 2,
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Blocks", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${blocks.sumOf { it.minutes }} min total",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = { blocks = blocks + newBlock() }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }
        itemsIndexed(blocks, key = { index, block -> block.id ?: "new-$index" }) { index, block ->
            BlockEditor(
                index = index,
                block = block,
                resources = resources,
                onChange = { update(index, it) },
                onMove = { move(index, it) },
                onRemove = { if (blocks.size > 1) blocks = blocks.toMutableList().apply { removeAt(index) } },
                canMoveUp = index > 0,
                canMoveDown = index < blocks.lastIndex,
                canRemove = blocks.size > 1,
            )
        }
        item {
            Button(
                onClick = {
                    onSave(RoutineEditorInput(routine?.id, name, description, category, blocks))
                    onBack()
                },
                enabled = name.isNotBlank() && blocks.all {
                    it.name.isNotBlank() && it.minutes in 1..240 && (it.bpm == null || it.bpm in 20..300)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save routine") }
        }
    }
}

@Composable
private fun BlockEditor(
    index: Int,
    block: RoutineBlockInput,
    resources: List<SessionResource>,
    onChange: (RoutineBlockInput) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
) {
    var expanded by rememberSaveable(block.id, index) { mutableStateOf(block.name.isBlank()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Block ${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(block.name.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (!expanded) {
                        Text(
                            buildString {
                                append("${block.minutes} min")
                                block.bpm?.let { append(" · $it BPM") }
                                block.skill?.let { append(" · ${skillLabel(it)}") }
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move block up")
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move block down")
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Edit block",
                    )
                }
            }
            if (expanded) {
                OutlinedTextField(
                    value = block.name,
                    onValueChange = { onChange(block.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Block name") },
                    singleLine = true,
                )
                SettingLabel("Duration")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onChange(block.copy(minutes = (block.minutes - 1).coerceAtLeast(1))) }) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Remove one minute")
                    }
                    Text("${block.minutes} min", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onChange(block.copy(minutes = (block.minutes + 1).coerceAtMost(240))) }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add one minute")
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 20).forEach { minutes ->
                            FilterChip(
                                selected = block.minutes == minutes,
                                onClick = { onChange(block.copy(minutes = minutes)) },
                                label = { Text("$minutes") },
                            )
                        }
                    }
                }
                SettingLabel("Skill")
                SkillChips(block.skill ?: "general") { onChange(block.copy(skill = it)) }
                SettingLabel("Metronome")
                BpmChips(block.bpm) { onChange(block.copy(bpm = it)) }
                SettingLabel("Practice resource")
                ResourceChips(resources, block.resourceId) { onChange(block.copy(resourceId = it)) }
                OutlinedTextField(
                    value = block.notes.orEmpty(),
                    onValueChange = { onChange(block.copy(notes = it.ifBlank { null })) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Optional notes") },
                    minLines = 2,
                )
                if (canRemove) {
                    TextButton(onClick = onRemove) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete block")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SkillChips(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SKILLS.forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

@Composable
private fun BpmChips(selected: Int?, onSelect: (Int?) -> Unit) {
    val options = (STANDARD_BPMS + listOfNotNull(selected)).distinct().sorted()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Free tempo") })
        options.forEach { bpm ->
            FilterChip(selected = selected == bpm, onClick = { onSelect(bpm) }, label = { Text("$bpm BPM") })
        }
    }
}

@Composable
private fun ResourceChips(resources: List<SessionResource>, selected: String?, onSelect: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("None") })
        resources.forEach { resource ->
            FilterChip(
                selected = selected == resource.id,
                onClick = { onSelect(resource.id) },
                label = { Text(resource.title) },
            )
        }
    }
}

private val SKILLS = listOf(
    "general" to "General",
    "arpegios" to "Arpeggios",
    "cambios-acordes" to "Chord changes",
    "patrones" to "Patterns",
    "velocidad" to "Speed",
    "tecnica" to "Technique",
    "repertorio" to "Repertoire",
)

private val STANDARD_BPMS = listOf(60, 80, 100, 120, 140)

private fun skillLabel(id: String): String = SKILLS.firstOrNull { it.first == id }?.second ?: id

private fun newBlock() = RoutineBlockInput(null, "", 5, null, null, "general", null)
