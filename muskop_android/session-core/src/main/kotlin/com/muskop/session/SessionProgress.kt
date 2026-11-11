package com.muskop.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class PracticeLogEntry(
    val date: String,
    val routineId: String,
    val routineName: String?,
    val minutes: Int,
    val completed: Boolean,
    val skillMinutes: Map<String, Int>,
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
        PracticeLogEntry(
            date = date,
            routineId = routineId,
            routineName = entry.stringValue("routineName"),
            minutes = minutes.coerceAtLeast(0),
            completed = entry["completed"]?.jsonPrimitive?.booleanOrNull ?: false,
            skillMinutes = entry.intMap("skillMinutes"),
        )
    }

fun SessionDocument.practiceGoals(): PracticeGoals {
    val goals = root["goals"] as? JsonObject ?: return PracticeGoals(0, 0, 0)
    return PracticeGoals(
        weekly = goals["weekly"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
        monthly = goals["monthly"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
        yearly = goals["yearly"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
    )
}

fun SessionDocument.skillGoals(): Map<String, Int> = root.intMap("skillGoals")

internal fun JsonObject.intMap(key: String): Map<String, Int> = (this[key] as? JsonObject)
    ?.mapNotNull { (skill, value) ->
        value.jsonPrimitive.intOrNull?.takeIf { it > 0 }?.let { minutes -> skill to minutes }
    }
    ?.toMap()
    .orEmpty()
