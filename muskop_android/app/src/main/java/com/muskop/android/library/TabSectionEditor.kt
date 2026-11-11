package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muskop.session.TabSectionInput
import com.muskop.session.TabNoteInput
import com.muskop.session.updateMeasureNote
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Composable
fun TabSectionEditor(
    sections: List<TabSectionInput>,
    content: kotlinx.serialization.json.JsonElement,
    onChange: (List<TabSectionInput>) -> Unit,
    onMeasureChange: (String, Int, JsonObject) -> Unit,
) {
    var selectedFret by remember { mutableIntStateOf(0) }
    var selectedFinger by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Sections")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..24).forEach { fret ->
                TextButton(onClick = { selectedFret = fret }) { Text(if (selectedFret == fret) "[$fret]" else "$fret") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(null to "—", "p" to "P", "i" to "I", "m" to "M", "a" to "A").forEach { (finger, label) ->
                TextButton(onClick = { selectedFinger = finger }) { Text(if (selectedFinger == finger) "[$label]" else label) }
            }
        }
        sections.forEachIndexed { index, section ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(section.name, { value -> onChange(sections.updated(index, section.copy(name = value))) }, Modifier.fillMaxWidth(), label = { Text("Name") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(section.kind, { value -> onChange(sections.updated(index, section.copy(kind = value))) }, Modifier.weight(1f), label = { Text("Type") })
                    OutlinedTextField(section.bpm?.toString().orEmpty(), { value -> onChange(sections.updated(index, section.copy(bpm = value.toIntOrNull()))) }, Modifier.weight(1f), label = { Text("BPM") })
                }
                OutlinedTextField(section.notes.orEmpty(), { value -> onChange(sections.updated(index, section.copy(notes = value.ifBlank { null }))) }, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 2)
                val sectionId = section.id
                val sectionJson = (content as? JsonObject)?.get("sections") as? JsonArray
                val measureList = sectionJson.orEmpty().mapNotNull { it as? JsonObject }
                    .firstOrNull { it["id"]?.toString()?.trim('"') == sectionId }
                    ?.get("measures") as? JsonArray
                if (sectionId != null && section.kind != "chords") {
                    Text("Measures")
                    measureList.orEmpty().forEachIndexed { measureIndex, element ->
                        val measure = element as? JsonObject ?: return@forEachIndexed
                        Text("Measure ${measureIndex + 1}")
                        TabMeasureGrid(measure) { beat, string, currentFret ->
                            val note = if (currentFret == null) TabNoteInput(string, selectedFret, selectedFinger) else null
                            onMeasureChange(sectionId, measureIndex, updateMeasureNote(measure, beat, string, note?.fret, note?.finger))
                        }
                    }
                    TextButton(onClick = {
                        val measures = measureList.orEmpty().toMutableList()
                        measures += JsonObject(mapOf("events" to JsonArray(emptyList())))
                        onMeasureChange(sectionId, measures.lastIndex, measures.last() as JsonObject)
                    }) { Text("+ Add measure") }
                }
                TextButton(onClick = { onChange(sections.toMutableList().apply { removeAt(index) }) }) { Text("Delete section") }
            }
        }
        TextButton(onClick = { onChange(sections + TabSectionInput(java.util.UUID.randomUUID().toString(), "fingerstyle", "New section", null, null)) }) { Text("+ Add section") }
    }
}

private fun <T> List<T>.updated(index: Int, item: T): List<T> = toMutableList().apply { set(index, item) }

