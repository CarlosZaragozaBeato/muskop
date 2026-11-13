package com.muskop.session

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ActivePracticeSnapshot(
    val kind: String,
    val routineId: String,
    val routineName: String,
    val skill: String?,
    val blockIndex: Int,
    val secondsLeft: Int?,
    val elapsedBySkillSeconds: Map<String, Int>,
    val practiceId: String = UUID.randomUUID().toString(),
    val resourceSeconds: Map<String, Int> = emptyMap(),
    val resourceLastPracticedAt: Map<String, String> = emptyMap(),
    val selectedResourceId: String? = null,
    val showResource: Boolean = true,
) : java.io.Serializable

fun SessionDocument.activePractice(): ActivePracticeSnapshot? {
    val active = root["activePractice"] as? JsonObject ?: return null
    val kind = active.stringValue("kind") ?: return null
    val routineId = active.stringValue("routineId") ?: return null
    val routineName = active.stringValue("routineName") ?: return null
    return ActivePracticeSnapshot(
        kind = kind,
        routineId = routineId,
        routineName = routineName,
        skill = active.stringValue("skill"),
        blockIndex = active["blockIndex"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
        secondsLeft = active["secondsLeft"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0),
        elapsedBySkillSeconds = active.intMap("elapsedBySkillSeconds"),
        practiceId = active.stringValue("practiceId") ?: UUID.randomUUID().toString(),
        resourceSeconds = active.intMap("resourceSeconds"),
        resourceLastPracticedAt = active.textMap("resourceLastPracticedAt"),
        selectedResourceId = active.stringValue("selectedResourceId"),
        showResource = (active["showResource"] as? JsonPrimitive)?.content != "false",
    )
}

fun SessionDocument.saveActivePractice(snapshot: ActivePracticeSnapshot): SessionDocument {
    require(snapshot.kind in setOf("free", "routine")) { "Invalid active practice type" }
    require(snapshot.routineId.isNotBlank()) { "routineId is required" }
    require(snapshot.practiceId.isNotBlank()) { "Practice id is required" }
    validateResourcePractice(snapshot.resourceSeconds, snapshot.resourceLastPracticedAt, snapshot.elapsedBySkillSeconds.values.sum())
    // Late autosaves after Finish must not resurrect a practice that is already recorded.
    if ((root["practiceLog"] as? JsonArray).orEmpty().any {
        (it as? JsonObject)?.stringValue("id") == snapshot.practiceId
    }) return this
    val existing = root["activePractice"] as? JsonObject
    val samePractice = existing?.stringValue("practiceId") == snapshot.practiceId ||
        (existing?.stringValue("practiceId") == null && existing?.stringValue("routineId") == snapshot.routineId && existing?.stringValue("kind") == snapshot.kind)
    val active = buildMap<String, kotlinx.serialization.json.JsonElement> {
        if (samePractice) putAll(existing.orEmpty())
        put("practiceId", JsonPrimitive(snapshot.practiceId))
        put("resourceSeconds", JsonObject(snapshot.resourceSeconds.filterValues { it > 0 }.mapValues { JsonPrimitive(it.value) }))
        put("resourceLastPracticedAt", JsonObject(snapshot.resourceLastPracticedAt.mapValues { JsonPrimitive(it.value) }))
        put("selectedResourceId", snapshot.selectedResourceId?.let(::JsonPrimitive) ?: JsonNull)
        put("showResource", JsonPrimitive(snapshot.showResource))
        put("kind", JsonPrimitive(snapshot.kind))
        put("routineId", JsonPrimitive(snapshot.routineId))
        put("routineName", JsonPrimitive(snapshot.routineName))
        put("skill", snapshot.skill?.takeIf(String::isNotBlank)?.let(::JsonPrimitive) ?: JsonNull)
        put("blockIndex", JsonPrimitive(snapshot.blockIndex.coerceAtLeast(0)))
        put("secondsLeft", snapshot.secondsLeft?.let { JsonPrimitive(it.coerceAtLeast(0)) } ?: JsonNull)
        put(
            "elapsedBySkillSeconds",
            JsonObject(snapshot.elapsedBySkillSeconds.filterValues { it > 0 }.mapValues { JsonPrimitive(it.value) }),
        )
        put("updatedAt", JsonPrimitive(Instant.now().toString()))
    }
    return withRootValue("activePractice", JsonObject(active))
}

fun SessionDocument.clearActivePractice(): SessionDocument = withRootValue("activePractice", JsonNull)

internal fun SessionDocument.withRootValue(key: String, value: kotlinx.serialization.json.JsonElement): SessionDocument =
    withRootValues(mapOf(key to value))

internal fun SessionDocument.withRootValues(
    values: Map<String, kotlinx.serialization.json.JsonElement>,
): SessionDocument {
    val user = root["user"] as? JsonObject
        ?: throw SessionFormatException("The session is missing the user object")
    val updatedUser = JsonObject(
        user.toMutableMap().apply { this["updatedAt"] = JsonPrimitive(Instant.now().toString()) },
    )
    return SessionDocument(
        JsonObject(
            root.toMutableMap().apply {
                putAll(values)
                this["user"] = updatedUser
            },
        ),
    )
}
