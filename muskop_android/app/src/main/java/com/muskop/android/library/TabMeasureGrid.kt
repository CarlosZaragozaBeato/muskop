package com.muskop.android.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun TabMeasureGrid(measure: JsonObject, onToggle: (Double, Int, Int?) -> Unit) {
    val events = measure["events"] as? JsonArray ?: JsonArray(emptyList())
    Row {
        Column {
            Box(Modifier.width(24.dp).height(20.dp))
            (1..6).forEach { string ->
                Box(Modifier.width(24.dp).height(48.dp), contentAlignment = Alignment.CenterStart) {
                    Text("$string", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(Modifier.height(20.dp)) {
                (0..15).forEach { slot ->
                    Text(if (slot % 4 == 0) "${slot / 4 + 1}" else "·", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            (1..6).forEach { string ->
                Row {
                (0..15).forEach { slot ->
                    val beat = 1.0 + slot / 4.0
                    val fret = events.firstOrNull { event ->
                        val objectEvent = event as? JsonObject ?: return@firstOrNull false
                        objectEvent["beat"]?.jsonPrimitive?.doubleOrNull == beat &&
                            (objectEvent["notes"] as? JsonArray)?.any { note ->
                                (note as? JsonObject)?.get("string")?.jsonPrimitive?.intOrNull == string
                            } == true
                    }?.let { event ->
                        ((event as JsonObject)["notes"] as JsonArray).firstOrNull { note ->
                            (note as? JsonObject)?.get("string")?.jsonPrimitive?.intOrNull == string
                        }?.let { (it as JsonObject)["fret"]?.jsonPrimitive?.intOrNull }
                    }
                    TextButton(onClick = { onToggle(beat, string, fret) }, modifier = Modifier.width(48.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text(fret?.toString() ?: "·") }
                }
            }
            }
        }
    }
}
