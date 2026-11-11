package com.muskop.android.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    Column {
        (1..6).forEach { string ->
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Text("$string")
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
                    TextButton(onClick = { onToggle(beat, string, fret) }) { Text(fret?.toString() ?: "·") }
                }
            }
        }
    }
}
