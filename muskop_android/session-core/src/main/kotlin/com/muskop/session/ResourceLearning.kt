package com.muskop.session

import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

enum class ResourceLearningStatus(val value: String) {
    NOT_STARTED("not-started"),
    LEARNING("learning"),
    PLAYABLE("playable"),
    MAINTAINING("maintaining"),
}

/** Missing or unsupported values do not imply that the user has never studied a resource. */
fun SessionResource.learningStatus(): ResourceLearningStatus? {
    val status = ((learning as? JsonObject)?.get("status") as? JsonPrimitive)
        ?.takeIf { it.isString }?.content
    return ResourceLearningStatus.entries.firstOrNull { it.value == status }
}

fun SessionResource.masteryPercent(): Int? = ((learning as? JsonObject)?.get("masteryPercent") as? JsonPrimitive)
    ?.takeUnless { it.isString }?.intOrNull?.takeIf { it in 0..100 }

/** Patch flags let one assessment change without replacing unsupported data in the other. */
data class ResourceLearningUpdate(
    val status: ResourceLearningStatus?,
    val masteryPercent: Int?,
    val updateStatus: Boolean = true,
    val updateMastery: Boolean = true,
)

fun SessionDocument.updateResourceLearning(resourceId: String, input: ResourceLearningUpdate): SessionDocument {
    require(!input.updateMastery || input.masteryPercent == null || input.masteryPercent in 0..100) {
        "Mastery percentage must be between 0 and 100"
    }
    val changes = buildMap<String, JsonElement> {
        if (input.updateStatus) put("status", input.status?.let { JsonPrimitive(it.value) } ?: JsonNull)
        if (input.updateMastery) put("masteryPercent", input.masteryPercent?.let(::JsonPrimitive) ?: JsonNull)
    }
    return updateLearningFields(resourceId, changes)
}

/** Changes only personal learning data, preserving content and unknown fields. Null clears the rating. */
fun SessionDocument.setResourceLearningStatus(resourceId: String, status: ResourceLearningStatus?): SessionDocument {
    return updateResourceLearning(resourceId, ResourceLearningUpdate(status, null, updateMastery = false))
}

private fun SessionDocument.updateLearningFields(resourceId: String, changes: Map<String, JsonElement>): SessionDocument {
    require(resourceId.isNotBlank()) { "A resource id is required" }
    val resources = root["resources"] as? JsonArray
        ?: throw SessionFormatException("The resources field must be a JSON array")
    val matches = resources.mapIndexedNotNull { index, item ->
        val resource = item as? JsonObject ?: return@mapIndexedNotNull null
        if ((resource["id"] as? JsonPrimitive)?.content == resourceId) index to resource else null
    }
    require(matches.size == 1) { "The resource is missing or its id is ambiguous" }
    val (index, resource) = matches.single()
    val existing = resource["learning"]
    val fields = existing as? JsonObject
    if (changes.isEmpty()) return this
    if ((existing == null || existing is JsonNull || fields != null) && changes.all { (key, value) ->
        fields?.get(key) == value || (value is JsonNull && fields?.get(key) == null)
    }) return this

    val learning = JsonObject((fields?.toMap() ?: emptyMap()) + changes + mapOf(
        "updatedAt" to JsonPrimitive(Instant.now().toString()),
    ))
    val updatedResource = JsonObject(resource + ("learning" to learning))
    val updated = resources.toMutableList().apply { this[index] = updatedResource }
    return withRootValue("resources", JsonArray(updated))
}
