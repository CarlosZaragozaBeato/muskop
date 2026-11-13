package com.muskop.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class PracticeLogEntry(
    val date: String,
    val routineId: String,
    val routineName: String?,
    val minutes: Int,
    val seconds: Int,
    val completed: Boolean,
    val skillMinutes: Map<String, Int>,
    val id: String? = null,
    val resourceSeconds: Map<String, Int> = emptyMap(),
    val resourceLastPracticedAt: Map<String, String> = emptyMap(),
)

data class PracticeGoals(
    val weekly: Int,
    val monthly: Int,
    val yearly: Int,
)

fun SessionDocument.practiceEntries(): List<PracticeLogEntry> = (root["practiceLog"] as? JsonArray)
    .orEmpty()
    .mapNotNull { item ->
        val entry = item as? JsonObject ?: return@mapNotNull null
        val date = entry.stringValue("date") ?: return@mapNotNull null
        val routineId = entry.stringValue("routineId") ?: return@mapNotNull null
        val minutes = entry["minutes"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
        val normalizedMinutes = minutes.coerceAtLeast(0)
        PracticeLogEntry(
            date = date,
            routineId = routineId,
            routineName = entry.stringValue("routineName"),
            minutes = normalizedMinutes,
            seconds = entry["seconds"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
                ?: normalizedMinutes * 60,
            completed = entry["completed"]?.jsonPrimitive?.booleanOrNull ?: false,
            skillMinutes = entry.intMap("skillMinutes"),
            id = entry.stringValue("id"),
            resourceSeconds = entry.intMap("resourceSeconds"),
            resourceLastPracticedAt = entry.textMap("resourceLastPracticedAt"),
        )
    }

fun Iterable<PracticeLogEntry>.totalWholeMinutes(): Int = sumOf(PracticeLogEntry::seconds) / 60

fun SessionDocument.practiceGoals(): PracticeGoals {
    val goals = root["goals"] as? JsonObject ?: return PracticeGoals(0, 0, 0)
    return PracticeGoals(
        weekly = goals["weekly"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
        monthly = goals["monthly"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
        yearly = goals["yearly"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
    )
}

fun SessionDocument.savePracticeGoals(goals: PracticeGoals): SessionDocument {
    require(goals.weekly >= 0 && goals.monthly >= 0 && goals.yearly >= 0) {
        "Practice goals cannot be negative"
    }
    val current = root["goals"] as? JsonObject
    val updated = JsonObject((current?.toMutableMap() ?: mutableMapOf()).apply {
        this["weekly"] = JsonPrimitive(goals.weekly)
        this["monthly"] = JsonPrimitive(goals.monthly)
        this["yearly"] = JsonPrimitive(goals.yearly)
    })
    return withRootValue("goals", updated)
}

fun SessionDocument.skillGoals(): Map<String, Int> = root.intMap("skillGoals")

internal fun JsonObject.intMap(key: String): Map<String, Int> = (this[key] as? JsonObject)
    ?.mapNotNull { (skill, value) ->
        value.jsonPrimitive.intOrNull?.takeIf { it > 0 }?.let { minutes -> skill to minutes }
    }
    ?.toMap()
    .orEmpty()

internal fun JsonObject.textMap(key: String): Map<String, String> = (this[key] as? JsonObject)
    ?.mapNotNull { (id, value) ->
        (value as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content?.let { id to it }
    }?.toMap().orEmpty()
