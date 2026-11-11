package com.muskop.session

import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class ResourceEditorInput(
    val id: String?,
    val title: String,
    val type: String,
    val category: String?,
    val tags: List<String>,
    val content: JsonElement,
)

fun SessionDocument.saveResource(input: ResourceEditorInput): SessionDocument {
    require(input.title.isNotBlank()) { "Resource title is required" }
    require(input.type.isNotBlank()) { "Resource type is required" }
    val resources = root["resources"] as? JsonArray
        ?: throw SessionFormatException("The resources field must be a JSON array")
    val id = input.id ?: UUID.randomUUID().toString()
    val existing = resources.mapNotNull { it as? JsonObject }.firstOrNull { it.stringValue("id") == id }
    val updated = JsonObject(
        (existing?.toMutableMap() ?: mutableMapOf()).apply {
            this["id"] = JsonPrimitive(id)
            this["title"] = JsonPrimitive(input.title.trim())
            this["type"] = JsonPrimitive(input.type.trim())
            this["category"] = input.category.trimOrNull().asJson()
            this["tags"] = JsonArray(input.tags.map(String::trim).filter(String::isNotEmpty).distinct().map(::JsonPrimitive))
            this["content"] = input.content
        },
    )
    val next = resources.map { element ->
        if ((element as? JsonObject)?.stringValue("id") == id) updated else element
    }.toMutableList()
    if (existing == null) next += updated
    return withRootValue("resources", JsonArray(next))
}

fun theoryContent(body: String): JsonObject = JsonObject(
    mapOf("kind" to JsonPrimitive("theory"), "body" to JsonPrimitive(body)),
)

private fun String?.trimOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.asJson(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull
