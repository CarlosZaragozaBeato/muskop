package com.muskop.session

import kotlinx.serialization.encodeToString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

const val CURRENT_SESSION_VERSION = 2

@OptIn(ExperimentalSerializationApi::class)
private val exportJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    explicitNulls = true
}

class SessionDocument internal constructor(
    internal val root: JsonObject,
) {
    val version: Int
        get() = root.getValue("muskopSession").jsonPrimitive.int

    val username: String
        get() = root.getValue("user").jsonObjectValue().getValue("username").jsonPrimitive.content

    fun inventory(): SessionInventory = SessionInventory(
        resources = root.arraySize("resources"),
        routines = root.arraySize("routines"),
        practiceEntries = root.arraySize("practiceLog"),
        projects = root.arraySize("projects"),
        trashItems = root.arraySize("trash"),
        unknownTopLevelKeys = root.keys - KNOWN_TOP_LEVEL_KEYS,
    )

    fun toJson(): String = exportJson.encodeToString(JsonObject.serializer(), root)

    internal fun toJsonObject(): JsonObject = root
}

data class SessionInventory(
    val resources: Int,
    val routines: Int,
    val practiceEntries: Int,
    val projects: Int,
    val trashItems: Int,
    val unknownTopLevelKeys: Set<String>,
)

internal val KNOWN_TOP_LEVEL_KEYS = setOf(
    "muskopSession",
    "user",
    "resources",
    "routines",
    "practiceLog",
    "experience",
    "bonusXp",
    "goals",
    "goalsClaimed",
    "achievements",
    "settings",
    "activePractice",
    "bestSessionMinutes",
    "challengesClaimed",
    "dayNotes",
    "projects",
    "schedule",
    "skillGoals",
    "skillGoalsClaimed",
    "streakFreezes",
    "trash",
)

internal fun JsonObject.arraySize(key: String): Int = (this[key] as? JsonArray)?.size ?: 0

internal fun JsonElement.jsonObjectValue(): JsonObject = this as? JsonObject
    ?: throw SessionFormatException("The session field must be a JSON object")

