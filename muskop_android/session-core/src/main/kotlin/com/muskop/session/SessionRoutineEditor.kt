package com.muskop.session

import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class RoutineBlockInput(
    val id: String?,
    val name: String,
    val minutes: Int,
    val bpm: Int?,
    val resourceId: String?,
    val skill: String?,
    val notes: String?,
)

data class RoutineEditorInput(
    val id: String?,
    val name: String,
    val description: String?,
    val category: String?,
    val blocks: List<RoutineBlockInput>,
)

fun SessionDocument.saveRoutine(input: RoutineEditorInput): SessionDocument {
    require(input.name.isNotBlank()) { "Routine name is required" }
    require(input.blocks.isNotEmpty()) { "A routine needs at least one block" }
    require(input.blocks.all { it.name.isNotBlank() && it.minutes in 1..240 }) {
        "Each block needs a name and a duration from 1 to 240 minutes"
    }
    require(input.blocks.all { it.bpm == null || it.bpm in 20..300 }) { "BPM must be between 20 and 300" }

    val routines = root["routines"] as? JsonArray
        ?: throw SessionFormatException("The routines field must be a JSON array")
    val routineId = input.id ?: UUID.randomUUID().toString()
    val existing = routines.mapNotNull { it as? JsonObject }.firstOrNull { it.stringValue("id") == routineId }
    val existingBlocks = existing?.objectList("blocks").orEmpty().associateBy { it.stringValue("id") }
    val updatedRoutine = JsonObject(
        (existing?.toMutableMap() ?: mutableMapOf()).apply {
            this["id"] = JsonPrimitive(routineId)
            this["name"] = JsonPrimitive(input.name.trim())
            this["description"] = input.description.trimOrNull().asJson()
            this["category"] = input.category.trimOrNull().asJson()
            this["blocks"] = JsonArray(input.blocks.map { block -> block.toJson(existingBlocks[block.id]) })
        },
    )
    val newRoutines = routines.map { item ->
        val routine = item as? JsonObject
        if (routine?.stringValue("id") == routineId) updatedRoutine else item
    }.toMutableList()
    if (existing == null) newRoutines += updatedRoutine
    return withRootValue("routines", JsonArray(newRoutines))
}

fun SessionDocument.deleteRoutine(routineId: String): SessionDocument {
    require(routineId.isNotBlank()) { "Routine id is required" }
    val routines = root["routines"] as? JsonArray
        ?: throw SessionFormatException("The routines field must be a JSON array")
    require(routines.any { (it as? JsonObject)?.stringValue("id") == routineId }) {
        "Routine not found"
    }
    val remaining = JsonArray(routines.filterNot { (it as? JsonObject)?.stringValue("id") == routineId })
    val pending = activePractice()
    return withRootValues(buildMap {
        put("routines", remaining)
        if (pending?.kind == "routine" && pending.routineId == routineId) {
            put("activePractice", JsonNull)
        }
    })
}

private fun RoutineBlockInput.toJson(existing: JsonObject?): JsonObject = JsonObject(
    (existing?.toMutableMap() ?: mutableMapOf()).apply {
        this["id"] = JsonPrimitive(id ?: UUID.randomUUID().toString())
        this["name"] = JsonPrimitive(name.trim())
        this["minutes"] = JsonPrimitive(minutes)
        this["bpm"] = bpm?.let(::JsonPrimitive) ?: JsonNull
        this["resourceId"] = resourceId.trimOrNull().asJson()
        this["skill"] = skill.trimOrNull().asJson()
        this["notes"] = notes.trimOrNull().asJson()
    },
)

private fun String?.trimOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.asJson() = this?.let(::JsonPrimitive) ?: JsonNull
