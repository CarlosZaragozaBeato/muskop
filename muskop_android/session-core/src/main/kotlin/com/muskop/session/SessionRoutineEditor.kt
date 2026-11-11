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
    require(input.name.isNotBlank()) { "El nombre de la rutina es obligatorio" }
    require(input.blocks.isNotEmpty()) { "La rutina necesita al menos un bloque" }
    require(input.blocks.all { it.name.isNotBlank() && it.minutes in 1..240 }) {
        "Cada bloque necesita nombre y una duración de 1 a 240 minutos"
    }
    require(input.blocks.all { it.bpm == null || it.bpm in 20..300 }) { "El BPM debe estar entre 20 y 300" }

    val routines = root["routines"] as? JsonArray
        ?: throw SessionFormatException("El campo routines debe ser una lista JSON")
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
