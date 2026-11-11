package com.muskop.android.library

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.session.SessionDocument
import com.muskop.session.SessionResource
import com.muskop.session.ResourceEditorInput
import com.muskop.session.TabSection
import com.muskop.session.resources
import com.muskop.session.tabSections
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun LibraryScreen(
    session: SessionDocument,
    padding: PaddingValues,
    onSaveResource: (ResourceEditorInput) -> Unit,
) {
    val resources = remember(session) { session.resources() }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingTheory by rememberSaveable { mutableStateOf(false) }
    val selected = resources.firstOrNull { it.id == selectedId }

    if (creatingTheory || editingId != null) {
        ResourceEditorScreen(
            resource = resources.firstOrNull { it.id == editingId },
            padding = padding,
            onSave = onSaveResource,
            onBack = { creatingTheory = false; editingId = null },
        )
    } else if (selected != null) {
        ResourceViewer(resource = selected, padding = padding, onBack = { selectedId = null }, onEdit = { editingId = it.id; selectedId = null })
    } else {
        ResourceList(resources = resources, padding = padding, onSelect = { selectedId = it.id }, onNewTheory = { creatingTheory = true })
    }
}

@Composable
private fun ResourceList(
    resources: List<SessionResource>,
    padding: PaddingValues,
    onSelect: (SessionResource) -> Unit,
    onNewTheory: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    val types = remember(resources) { resources.map(SessionResource::type).distinct().sorted() }
    val tags = remember(resources) { resources.flatMap(SessionResource::tags).distinct().sorted() }
    val filtered = remember(resources, query, selectedType, selectedTag) {
        resources.filter { resource ->
            val matchesQuery = query.isBlank() || resource.title.contains(query, ignoreCase = true)
            val matchesType = selectedType == null || resource.type == selectedType
            val matchesTag = selectedTag == null || selectedTag in resource.tags
            matchesQuery && matchesType && matchesTag
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Library", style = MaterialTheme.typography.headlineMedium)
        }
        item { TextButton(onClick = onNewTheory) { Text("+ New theory note") } }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search resources") },
            )
        }
        if (types.isNotEmpty()) {
            item {
                FilterRow(
                    label = "Type",
                    values = types,
                    selected = selectedType,
                    onSelect = { selectedType = if (selectedType == it) null else it },
                )
            }
        }
        if (tags.isNotEmpty()) {
            item {
                FilterRow(
                    label = "Tags",
                    values = tags,
                    selected = selectedTag,
                    onSelect = { selectedTag = if (selectedTag == it) null else it },
                )
            }
        }
        if (filtered.isEmpty()) {
            item { Text("No resources match these filters.") }
        }
        items(filtered, key = SessionResource::id) { resource ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(resource) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(resource.title, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { onSelect(resource) }, label = { Text(resource.type) })
                        resource.category?.let { category ->
                            AssistChip(onClick = { onSelect(resource) }, label = { Text(category) })
                        }
                    }
                    if (resource.tags.isNotEmpty()) {
                        Text(resource.tags.joinToString(separator = " · "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    values: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(values, key = { it }) { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(value) },
                )
            }
        }
    }
}

@Composable
private fun ResourceViewer(resource: SessionResource, padding: PaddingValues, onBack: () -> Unit, onEdit: (SessionResource) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("← Back to library") } }
        item { TextButton(onClick = { onEdit(resource) }) { Text("Edit") } }
        item { Text(resource.title, style = MaterialTheme.typography.headlineSmall) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(resource.type) })
                resource.category?.let { category -> AssistChip(onClick = {}, label = { Text(category) }) }
            }
        }
        if (resource.tags.isNotEmpty()) {
            item { Text("Tags: ${resource.tags.joinToString()}") }
        }
        item { ResourcePreview(resource) }
    }
}

@Composable
fun ResourcePreview(resource: SessionResource) {
    when {
        resource.type.equals("TAB", ignoreCase = true) -> {
            val sections = resource.tabSections()
            if (sections.isEmpty()) {
                Text("This tablature has no visible sections.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    sections.forEach { section -> TabSectionViewer(section) }
                }
            }
        }
        resource.type.equals("THEORY", ignoreCase = true) -> Text(theoryBody(resource))
        else -> Text("This resource type is preserved and will receive its own viewer in a future iteration.")
    }
}

@Composable
private fun TabSectionViewer(section: TabSection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(section.name, style = MaterialTheme.typography.titleMedium)
            Text("${section.kind} · ${section.bpm?.let { "$it BPM" } ?: "document tempo"}")
            section.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (section.measures.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    section.measures.forEachIndexed { index, measure ->
                        TabMeasure(measure = measure, number = index + 1)
                    }
                }
            }
            if (section.chords.isNotEmpty()) {
                Text(section.chords.joinToString(separator = " · ") { chord ->
                    chord["name"]?.jsonPrimitive?.contentOrNull ?: "Chord"
                })
            }
        }
    }
}

@Composable
private fun TabMeasure(measure: JsonObject, number: Int) {
    val notes = remember(measure) { notesIn(measure) }
    val labelColor = MaterialTheme.colorScheme.onSurface
    val lineColor = MaterialTheme.colorScheme.outline
    val noteColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.width(240.dp)) {
        Text("Measure $number", style = MaterialTheme.typography.labelMedium)
        Canvas(modifier = Modifier.fillMaxWidth().height(132.dp)) {
            val left = 18f
            val top = 18f
            val right = size.width - 12f
            val bottom = size.height - 18f
            val spacing = (bottom - top) / 5f
            repeat(6) { index ->
                val y = top + index * spacing
                drawLine(lineColor, Offset(left, y), Offset(right, y), strokeWidth = 1.5f)
            }
            repeat(5) { index ->
                val x = left + (right - left) * index / 4f
                drawLine(lineColor.copy(alpha = 0.35f), Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
            }
            val paint = Paint().apply {
                color = labelColor.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = 28f
                isAntiAlias = true
            }
            notes.forEach { note ->
                val x = left + ((note.beat - 1.0).coerceIn(0.0, 4.0) / 4.0 * (right - left)).toFloat()
                val y = top + (note.string - 1).coerceIn(0, 5) * spacing
                drawCircle(noteColor, radius = 14f, center = Offset(x, y))
                paint.color = Color.White.toArgb()
                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(note.fret.toString(), x, y + 9f, paint) }
            }
        }
    }
}

private data class TabNote(val beat: Double, val string: Int, val fret: Int)

private fun notesIn(measure: JsonObject): List<TabNote> = (measure["events"] as? JsonArray)
    .orEmpty()
    .flatMap { eventElement ->
        val event = eventElement as? JsonObject ?: return@flatMap emptyList()
        val beat = event["beat"]?.jsonPrimitive?.doubleOrNull ?: return@flatMap emptyList()
        (event["notes"] as? JsonArray).orEmpty().mapNotNull { noteElement ->
            val note = noteElement as? JsonObject ?: return@mapNotNull null
            val string = note["string"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val fret = note["fret"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            TabNote(beat = beat, string = string, fret = fret)
        }
    }

private fun theoryBody(resource: SessionResource): String = (resource.content as? JsonObject)
    ?.get("body")
    ?.jsonPrimitive
    ?.contentOrNull
    ?: "This theory resource has no text content."
