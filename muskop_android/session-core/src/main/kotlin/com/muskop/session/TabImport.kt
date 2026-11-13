package com.muskop.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** Check the shape needed by the reader while preserving the entire input tree. */
fun importTabDocument(raw: String): JsonObject {
    val document = try { Json.parseToJsonElement(raw) as? JsonObject } catch (_: IllegalArgumentException) { null }
        ?: throw IllegalArgumentException("Enter a valid JSON object.")
    require((document["version"] as? JsonPrimitive)?.intOrNull == 2) { "Expected a tablature document with version 2." }
    require((document["title"] as? JsonPrimitive)?.let { it.isString && it.content.isNotBlank() } == true) { "The tablature needs a title." }
    document.checkText("category", "Document")
    document.checkText("timeSignature", "Document")
    document.checkNumber("baseBpm", "Document")
    document.array("tuning", "Document")?.forEachIndexed { i, item ->
        require(item is JsonPrimitive && item.isString) { "Tuning: item ${i + 1} must be text." }
    }
    val sections = document.array("sections", "Document") ?: throw IllegalArgumentException("The tablature needs a sections array.")
    sections.forEachIndexed { si, item ->
        val where = "Section ${si + 1}"
        val section = item as? JsonObject ?: throw IllegalArgumentException("$where must be an object.")
        listOf("id", "kind", "name", "notes").forEach { section.checkText(it, where) }
        section.checkNumber("bpm", where)
        section.array("measures", where)?.forEachIndexed { mi, measureItem ->
            val location = "$where, measure ${mi + 1}"
            val measure = measureItem as? JsonObject ?: throw IllegalArgumentException("$location must be an object.")
            measure.array("events", location)?.forEachIndexed { ei, eventItem ->
                val eventLocation = "$location, event ${ei + 1}"
                val event = eventItem as? JsonObject ?: throw IllegalArgumentException("$eventLocation must be an object.")
                event.checkNumber("beat", eventLocation, required = true)
                event.checkText("duration", eventLocation)
                event.array("notes", eventLocation)?.forEachIndexed { ni, noteItem ->
                    val noteLocation = "$eventLocation, note ${ni + 1}"
                    val note = noteItem as? JsonObject ?: throw IllegalArgumentException("$noteLocation must be an object.")
                    note.checkInteger("string", noteLocation)
                    note.checkInteger("fret", noteLocation)
                    note.checkText("finger", noteLocation)
                    note.checkText("technique", noteLocation)
                }
            }
        }
        section.array("chords", where)?.forEachIndexed { ci, chord ->
            require(chord is JsonObject) { "$where, chord ${ci + 1} must be an object." }
        }
    }
    document.importedResourceTags()
    readMusicalMetadata(document, validateSectionReferences = true)
    return document
}

/** Maps the portable document into editor data without rebuilding its musical content. */
fun importTabResource(raw: String): ResourceEditorInput {
    val document = importTabDocument(raw)
    return ResourceEditorInput(
        id = null,
        title = (document["title"] as JsonPrimitive).content,
        type = "TAB",
        category = (document["category"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
        tags = document.importedResourceTags(),
        content = ensureTabSectionIds(document),
    )
}

private fun JsonObject.array(key: String, where: String): JsonArray? {
    val value = this[key] ?: return null
    require(value is JsonArray) { "$where: $key must be an array." }
    return value
}

private fun JsonObject.checkText(key: String, where: String) {
    val value = this[key] ?: return
    require(value is JsonNull || value is JsonPrimitive && value.isString) { "$where: $key must be text." }
}

private fun JsonObject.checkNumber(key: String, where: String, required: Boolean = false) {
    val value = this[key]
    if (!required && (value == null || value is JsonNull)) return
    require(value is JsonPrimitive && !value.isString && value.doubleOrNull?.isFinite() == true) { "$where: $key must be a number." }
}

private fun JsonObject.checkInteger(key: String, where: String) {
    val value = this[key]
    require(value is JsonPrimitive && !value.isString && value.intOrNull != null) { "$where: $key must be an integer." }
}
