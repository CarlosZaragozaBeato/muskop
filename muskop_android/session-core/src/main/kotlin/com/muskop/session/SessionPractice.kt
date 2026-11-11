package com.muskop.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class PracticeBlock(
    val id: String,
    val name: String,
    val minutes: Int,
    val bpm: Int?,
    val resourceId: String?,
    val skill: String?,
    val notes: String?,
)

data class PracticeRoutine(
    val id: String,
    val name: String,
    val description: String?,
    val category: String?,
    val blocks: List<PracticeBlock>,
)

data class PracticeEntryInput(
    val date: String,
    val routineId: String,
    val routineName: String?,
    val minutes: Int,
    val completed: Boolean,
    val skillMinutes: Map<String, Int>,
)

fun SessionDocument.routines(): List<PracticeRoutine> = (root["routines"] as? JsonArray)
    .orEmpty()
    .mapNotNull { item ->
        val routine = item as? JsonObject ?: return@mapNotNull null
        val id = routine.stringValue("id") ?: return@mapNotNull null
        val name = routine.stringValue("name") ?: return@mapNotNull null
        PracticeRoutine(
            id = id,
            name = name,
            description = routine.stringValue("description"),
            category = routine.stringValue("category"),
            blocks = routine.objectList("blocks").mapNotNull blockMap@{ block ->
                val blockId = block.stringValue("id") ?: return@blockMap null
                PracticeBlock(
                    id = blockId,
                    name = block.stringValue("name") ?: "Block",
                    minutes = block["minutes"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(1) ?: 5,
                    bpm = block["bpm"]?.jsonPrimitive?.intOrNull,
                    resourceId = block.stringValue("resourceId"),
                    skill = block.stringValue("skill"),
                    notes = block.stringValue("notes"),
                )
            },
        )
    }

fun PracticeRoutine.totalMinutes(): Int = blocks.sumOf(PracticeBlock::minutes)

fun SessionDocument.recordPractice(entry: PracticeEntryInput): SessionDocument {
    require(entry.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Date must use the YYYY-MM-DD format" }
    require(entry.routineId.isNotBlank()) { "routineId is required" }
    require(entry.minutes > 0) { "Practice minutes must be positive" }
    require(entry.skillMinutes.values.all { it >= 0 }) { "Skill minutes cannot be negative" }

    val practiceLog = root["practiceLog"] as? JsonArray
        ?: throw SessionFormatException("The practiceLog field must be a JSON array")
    val normalizedSkills = entry.skillMinutes.filterValues { it > 0 }
    val newEntry = buildMap<String, kotlinx.serialization.json.JsonElement> {
        put("date", JsonPrimitive(entry.date))
        put("routineId", JsonPrimitive(entry.routineId))
        entry.routineName?.takeIf(String::isNotBlank)?.let { put("routineName", JsonPrimitive(it)) }
        put("minutes", JsonPrimitive(entry.minutes))
        put("completed", JsonPrimitive(entry.completed))
        put(
            "skillMinutes",
            JsonObject(normalizedSkills.mapValues { (_, minutes) -> JsonPrimitive(minutes) }),
        )
    }
    return withRootValues(
        mapOf(
            "practiceLog" to JsonArray(practiceLog + JsonObject(newEntry)),
            "activePractice" to JsonNull,
        ),
    )
}
