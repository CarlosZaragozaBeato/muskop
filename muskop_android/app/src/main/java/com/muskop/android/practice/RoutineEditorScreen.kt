package com.muskop.android.practice

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var name by remember(routine) { mutableStateOf(routine?.name ?: "") }
    var description by remember(routine) { mutableStateOf(routine?.description.orEmpty()) }
    var category by remember(routine) { mutableStateOf(routine?.category.orEmpty()) }
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
        blocks = blocks.toMutableList().apply {
            val item = removeAt(index)
            add(target, item)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("← Volver a rutinas") } }
        item { Text(if (routine == null) "Nueva rutina" else "Editar rutina") }
        item {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
        }
        item {
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Categoría") }, singleLine = true)
        }
        item {
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Descripción") }, minLines = 2)
        }
        itemsIndexed(blocks) { index, block ->
            BlockEditor(
                index = index,
                block = block,
                resources = resources,
                onChange = { update(index, it) },
                onMove = { move(index, it) },
                onRemove = { if (blocks.size > 1) blocks = blocks.toMutableList().apply { removeAt(index) } },
                canMoveUp = index > 0,
                canMoveDown = index < blocks.lastIndex,
            )
        }
        item { TextButton(onClick = { blocks = blocks + newBlock() }) { Text("+ Añadir bloque") } }
        item {
            Button(
                onClick = {
                    onSave(RoutineEditorInput(routine?.id, name, description, category, blocks))
                    onBack()
                },
                enabled = name.isNotBlank() && blocks.all { it.name.isNotBlank() && it.minutes in 1..240 },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar rutina") }
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Bloque ${index + 1}")
        OutlinedTextField(block.name, { onChange(block.copy(name = it)) }, Modifier.fillMaxWidth(), label = { Text("Nombre del bloque") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = block.minutes.toString(),
                onValueChange = { value -> onChange(block.copy(minutes = value.toIntOrNull() ?: 0)) },
                modifier = Modifier.weight(1f),
                label = { Text("Minutos") },
                singleLine = true,
            )
            OutlinedTextField(
                value = block.bpm?.toString().orEmpty(),
                onValueChange = { value -> onChange(block.copy(bpm = value.toIntOrNull())) },
                modifier = Modifier.weight(1f),
                label = { Text("BPM") },
                singleLine = true,
            )
        }
        OutlinedTextField(block.skill.orEmpty(), { onChange(block.copy(skill = it.ifBlank { null })) }, Modifier.fillMaxWidth(), label = { Text("Habilidad") }, singleLine = true)
        OutlinedTextField(block.notes.orEmpty(), { onChange(block.copy(notes = it.ifBlank { null })) }, Modifier.fillMaxWidth(), label = { Text("Indicaciones") }, minLines = 2)
        Text("Recurso asociado")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onChange(block.copy(resourceId = null)) }) { Text("Ninguno") }
            resources.forEach { resource ->
                TextButton(onClick = { onChange(block.copy(resourceId = resource.id)) }) {
                    Text(if (block.resourceId == resource.id) "✓ ${resource.title}" else resource.title)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Text("↑") }
            TextButton(onClick = { onMove(1) }, enabled = canMoveDown) { Text("↓") }
            TextButton(onClick = onRemove, enabled = canMoveUp || canMoveDown) { Text("Eliminar") }
        }
    }
}

private fun newBlock() = RoutineBlockInput(null, "", 5, null, null, "general", null)
