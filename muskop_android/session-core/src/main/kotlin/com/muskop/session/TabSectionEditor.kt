package com.muskop.session

import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement

data class TabSectionInput(val id: String?, val kind: String, val name: String, val bpm: Int?, val notes: String?)

fun updateTabSectionMeasures(content: JsonElement, sectionId: String, measureIndex: Int, measure: JsonObject): JsonElement {
    val document = content as? JsonObject ?: return content
    val sections = document["sections"] as? JsonArray ?: return content
    val updatedSections = sections.map { item ->
        val section = item as? JsonObject ?: return@map item
        if (section.stringValue("id") != sectionId) return@map item
        val measures = (section["measures"] as? JsonArray)?.toMutableList() ?: mutableListOf()
        when {
            measureIndex in measures.indices -> measures[measureIndex] = measure
            measureIndex == measures.size -> measures += measure
        }
        JsonObject(section.toMutableMap().apply { this["measures"] = JsonArray(measures) })
    }.let { current ->
        if (current.any { (it as? JsonObject)?.stringValue("id") == sectionId }) current
        else current + JsonObject(mapOf(
            "id" to JsonPrimitive(sectionId),
            "kind" to JsonPrimitive("fingerstyle"),
            "name" to JsonPrimitive("Nueva sección"),
            "measures" to JsonArray(listOf(measure)),
        ))
    }
    return JsonObject(document.toMutableMap().apply { this["sections"] = JsonArray(updatedSections) })
}

fun updateTabSections(content: kotlinx.serialization.json.JsonElement, sections: List<TabSectionInput>): JsonElement {
    val document = content as? JsonObject ?: return content
    val existing = document.objectList("sections").associateBy { it.stringValue("id") }
    return JsonObject(document.toMutableMap().apply {
        this["sections"] = JsonArray(sections.map { input ->
            val id = input.id ?: UUID.randomUUID().toString()
            JsonObject((existing[id]?.toMutableMap() ?: mutableMapOf()).apply {
                this["id"] = JsonPrimitive(id)
                this["kind"] = JsonPrimitive(input.kind)
                this["name"] = JsonPrimitive(input.name)
                this["bpm"] = input.bpm?.let(::JsonPrimitive) ?: JsonNull
                this["notes"] = input.notes?.trim()?.takeIf(String::isNotEmpty)?.let(::JsonPrimitive) ?: JsonNull
                if (!containsKey("measures") && input.kind != "chords") this["measures"] = JsonArray(emptyList())
            })
        })
    })
}
