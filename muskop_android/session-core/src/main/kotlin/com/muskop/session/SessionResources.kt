package com.muskop.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class SessionResource(
    val id: String,
    val title: String,
    val type: String,
    val category: String?,
    val tags: List<String>,
    val content: JsonElement,
    val exercise: JsonObject?,
    val learning: JsonElement? = null,
    val organization: JsonElement? = null,
)

data class TabSection(
    val id: String?,
    val kind: String,
    val name: String,
    val bpm: Int?,
    val notes: String?,
    val measures: List<JsonObject>,
    val chords: List<JsonObject>,
)

fun SessionDocument.resources(): List<SessionResource> = (root["resources"] as? JsonArray)
    .orEmpty()
    .mapNotNull { item ->
        val resource = item as? JsonObject ?: return@mapNotNull null
        val id = resource.stringValue("id") ?: return@mapNotNull null
        val title = resource.stringValue("title") ?: return@mapNotNull null
        val type = resource.stringValue("type") ?: return@mapNotNull null
        SessionResource(
            id = id,
            title = title,
            type = type,
            category = resource.stringValue("category"),
            tags = resource.stringList("tags"),
            content = resource["content"] ?: JsonObject(emptyMap()),
            exercise = resource["exercise"] as? JsonObject,
            learning = resource["learning"],
            organization = resource["organization"],
        )
    }

fun SessionResource.tabSections(): List<TabSection> {
    if (!type.equals("TAB", ignoreCase = true)) return emptyList()
    val document = content as? JsonObject ?: return emptyList()
    return (document["sections"] as? JsonArray).orEmpty().mapNotNull { item ->
        val section = item as? JsonObject ?: return@mapNotNull null
        TabSection(
            id = section.stringValue("id"),
            kind = section.stringValue("kind") ?: "tab",
            name = section.stringValue("name") ?: "Untitled",
            bpm = section["bpm"]?.jsonPrimitive?.intOrNull,
            notes = section.stringValue("notes"),
            measures = section.objectList("measures"),
            chords = section.objectList("chords"),
        )
    }
}

internal fun JsonObject.stringValue(key: String): String? = this[key]
    ?.jsonPrimitive
    ?.contentOrNull
    ?.takeIf(String::isNotBlank)

internal fun JsonObject.stringList(key: String): List<String> = (this[key] as? JsonArray)
    .orEmpty()
    .mapNotNull { item -> item.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }

internal fun JsonObject.objectList(key: String): List<JsonObject> = (this[key] as? JsonArray)
    .orEmpty()
    .mapNotNull { item -> item as? JsonObject }
