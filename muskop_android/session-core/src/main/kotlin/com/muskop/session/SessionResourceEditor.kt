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

fun SessionDocument.deleteResource(resourceId: String): SessionDocument {
    require(resourceId.isNotBlank()) { "Resource id is required" }
    val resources = root["resources"] as? JsonArray
        ?: throw SessionFormatException("The resources field must be a JSON array")
    val original = resources.filterIsInstance<JsonObject>().firstOrNull { it.stringValue("id") == resourceId }
        ?: throw IllegalArgumentException("Resource not found")
    val trash = root["trash"] as? JsonArray ?: JsonArray(emptyList())
    val archived = JsonObject(mapOf(
        "id" to JsonPrimitive(UUID.randomUUID().toString()),
        "kind" to JsonPrimitive("resource"),
        "resource" to original,
    ))
    val routines = root["routines"] as? JsonArray
        ?: throw SessionFormatException("The routines field must be a JSON array")
    val updatedRoutines = JsonArray(routines.map routineLoop@{ item ->
        val routine = item as? JsonObject ?: return@routineLoop item
        val blocks = routine["blocks"] as? JsonArray ?: return@routineLoop item
        val updatedBlocks = JsonArray(blocks.map blockLoop@{ entry ->
            val block = entry as? JsonObject ?: return@blockLoop entry
            if (block.stringValue("resourceId") != resourceId) entry
            else JsonObject(block.toMutableMap().apply { this["resourceId"] = JsonNull })
        })
        JsonObject(routine.toMutableMap().apply { this["blocks"] = updatedBlocks })
    })
    return withRootValues(mapOf(
        "resources" to JsonArray(resources.filterNot { (it as? JsonObject)?.stringValue("id") == resourceId }),
        "trash" to JsonArray(trash + archived),
        "routines" to updatedRoutines,
    ))
}

fun theoryContent(body: String, previous: JsonElement? = null): JsonObject = JsonObject(
    ((previous as? JsonObject)?.toMap() ?: emptyMap()) +
        mapOf("kind" to JsonPrimitive("theory"), "body" to JsonPrimitive(body)),
)

private fun String?.trimOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.asJson(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull
