package com.muskop.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class TabNoteInput(val string: Int, val fret: Int, val finger: String? = null)

fun updateMeasureEvent(measure: JsonObject, beat: Double, notes: List<TabNoteInput>): JsonObject {
    require(beat in 1.0..4.75 && (beat * 4).toInt().toDouble() == beat * 4) { "Invalid beat" }
    require(notes.all { it.string in 1..6 && it.fret in 0..24 }) { "Note out of range" }
    val events = (measure["events"] as? JsonArray).orEmpty()
    val existing = events.firstOrNull { event ->
        (event as? JsonObject)?.get("beat")?.jsonPrimitive?.doubleOrNull == beat
    } as? JsonObject
    val replacement = existing?.let { JsonObject(it.toMutableMap().apply {
        this["notes"] = JsonArray(notes.map { note -> note.toJson(existingNoteAt(existing, note.string)) })
    }) } ?: JsonObject(mapOf(
        "beat" to JsonPrimitive(beat),
        "duration" to JsonPrimitive("quarter"),
        "notes" to JsonArray(notes.map { it.toJson(null) }),
    ))
    val next = events.filterNot { event -> (event as? JsonObject)?.get("beat")?.jsonPrimitive?.doubleOrNull == beat }.toMutableList()
    if (notes.isNotEmpty()) next += replacement
    return JsonObject(measure.toMutableMap().apply { this["events"] = JsonArray(next) })
}

fun updateMeasureNote(
    measure: JsonObject,
    beat: Double,
    string: Int,
    fret: Int?,
    finger: String?,
): JsonObject {
    require(beat in 1.0..4.75 && (beat * 4).toInt().toDouble() == beat * 4) { "Invalid beat" }
    require(string in 1..6) { "Invalid string" }
    require(fret == null || fret in 0..24) { "Invalid fret" }
    require(finger == null || finger in setOf("p", "i", "m", "a")) { "Invalid fingering" }
    val events = (measure["events"] as? JsonArray).orEmpty()
    val matchingEvents = events.mapNotNull { it as? JsonObject }.filter {
        it["beat"]?.jsonPrimitive?.doubleOrNull == beat
    }
    val event = matchingEvents.firstOrNull { existingNoteAt(it, string) != null } ?: matchingEvents.firstOrNull()
    val oldNote = existingNoteAt(event ?: JsonObject(emptyMap()), string)
    val notes = (event?.get("notes") as? JsonArray).orEmpty()
        .filterNot { (it as? JsonObject)?.get("string")?.jsonPrimitive?.intOrNull == string }
        .toMutableList()
    if (fret != null) {
        notes += JsonObject((oldNote?.toMutableMap() ?: mutableMapOf()).apply {
            this["string"] = JsonPrimitive(string)
            this["fret"] = JsonPrimitive(fret)
            if (finger != null) this["finger"] = JsonPrimitive(finger)
        })
    }
    val nextEvents = events.toMutableList()
    val eventIndex = events.indexOfFirst { it === event }
    if (eventIndex >= 0) nextEvents.removeAt(eventIndex)
    if (notes.isNotEmpty() || event != null) {
        val updatedEvent = if (event == null) {
            JsonObject(mapOf(
                "beat" to JsonPrimitive(beat),
                "duration" to JsonPrimitive("quarter"),
                "notes" to JsonArray(notes),
            ))
        } else {
            JsonObject(event.toMutableMap().apply { this["notes"] = JsonArray(notes) })
        }
        if (eventIndex >= 0) nextEvents.add(eventIndex, updatedEvent) else nextEvents.add(updatedEvent)
    }
    return JsonObject(measure.toMutableMap().apply { this["events"] = JsonArray(nextEvents) })
}

private fun existingNoteAt(event: JsonObject, string: Int): JsonObject? =
    (event["notes"] as? JsonArray).orEmpty().firstOrNull { note ->
        (note as? JsonObject)?.get("string")?.jsonPrimitive?.intOrNull == string
    } as? JsonObject

private fun TabNoteInput.toJson(existing: JsonObject?): JsonObject = JsonObject(
    (existing?.toMutableMap() ?: mutableMapOf()).apply {
        this["string"] = JsonPrimitive(string)
        this["fret"] = JsonPrimitive(fret)
        if (finger != null) this["finger"] = JsonPrimitive(finger)
    },
)
