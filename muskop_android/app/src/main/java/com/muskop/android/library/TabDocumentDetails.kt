package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private fun JsonObject.value(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

@Composable
fun TabDocumentDetails(content: JsonElement) {
    val document = content as? JsonObject ?: return
    val tuning = (document["tuning"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    var expanded by remember(content) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val signature = document.value("timeSignature")
        val bpm = document.value("baseBpm")
        val summary = listOfNotNull(
            signature,
            bpm?.let { "$it BPM" },
            tuning?.takeIf { it.isNotEmpty() }?.let { "${it.size} strings" },
        )
        if (summary.isNotEmpty()) {
            Text(summary.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        tuning?.takeIf { it.isNotEmpty() }?.let {
            TextButton(onClick = { expanded = !expanded },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text(if (expanded) "Hide tuning" else "Tuning")
            }
            if (expanded) {
                Text("Low to high: ${it.joinToString(" · ")}", style = MaterialTheme.typography.bodyMedium)
            }
            if (it.size > 24) Text("Tunings above 24 strings are shown in the note details.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TabMeasureDetails(measure: JsonObject, number: Int, initiallyExpanded: Boolean = false) {
    val events = (measure["events"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    if (events.isEmpty()) {
        Text("No notes in this measure.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var expanded by remember(measure, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
        TextButton(onClick = { expanded = !expanded },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
            Text(if (expanded) "Hide note details · Measure $number" else "Note details · Measure $number")
        }
        if (expanded) SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                events.forEach { event ->
                    val beat = event.value("beat") ?: "?"
                    val duration = event.value("duration")
                    val notes = (event["notes"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                    val details = notes.map { note ->
                        val parts = mutableListOf("string ${note.value("string") ?: "?"}, fret ${note.value("fret") ?: "?"}")
                        note.value("finger")?.let { parts += "finger $it" }
                        note.value("technique")?.let { parts += it }
                        parts.joinToString(" · ")
                    }
                    Text("Beat $beat${duration?.let { " · $it" }.orEmpty()}${if (details.isEmpty()) " · rest" else "\n" + details.joinToString("\n")}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun TabChordDetails(chord: JsonObject) {
    val frets = (chord["frets"] as? JsonArray)?.joinToString(" ") { (it as? JsonPrimitive)?.contentOrNull ?: "x" }
    val details = listOfNotNull(chord.value("name") ?: "Chord", frets?.let { "frets (low to high): $it" }, chord.value("baseFret")?.let { "base fret $it" }, chord.value("beats")?.let { "$it beats" })
    Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
}
