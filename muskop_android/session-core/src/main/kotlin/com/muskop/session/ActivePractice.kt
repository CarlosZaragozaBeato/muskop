package com.muskop.session

import java.time.Instant
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
)

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
    )
}

fun SessionDocument.saveActivePractice(snapshot: ActivePracticeSnapshot): SessionDocument {
    require(snapshot.kind in setOf("free", "routine")) { "Tipo de práctica activa no válido" }
    require(snapshot.routineId.isNotBlank()) { "routineId es obligatorio" }
    val active = buildMap<String, kotlinx.serialization.json.JsonElement> {
        put("kind", JsonPrimitive(snapshot.kind))
        put("routineId", JsonPrimitive(snapshot.routineId))
        put("routineName", JsonPrimitive(snapshot.routineName))
        snapshot.skill?.takeIf(String::isNotBlank)?.let { put("skill", JsonPrimitive(it)) }
        put("blockIndex", JsonPrimitive(snapshot.blockIndex.coerceAtLeast(0)))
        snapshot.secondsLeft?.let { put("secondsLeft", JsonPrimitive(it.coerceAtLeast(0))) }
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
        ?: throw SessionFormatException("Falta el objeto user en la sesión")
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
