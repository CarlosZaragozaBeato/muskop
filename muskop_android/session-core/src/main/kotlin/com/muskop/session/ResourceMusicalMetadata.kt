package com.muskop.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Read models only. The original JSON remains the source of truth when saving. */
data class ResourceMusicalMetadata(
    val role: String?,
    val difficulty: ResourceDifficulty?,
    val concepts: List<ResourceMusicalConcept>,
)

data class ResourceDifficulty(val level: Int, val reasons: List<String>)

data class ResourceMusicalConcept(
    val id: String,
    val label: String,
    val relation: String,
    val sectionIds: List<String>,
)

/** Old sessions and metadata from a future producer must remain readable and exportable. */
fun SessionResource.musicalMetadata(): ResourceMusicalMetadata? = runCatching {
    readMusicalMetadata(content as? JsonObject ?: return null)
}.getOrNull()

internal fun readMusicalMetadata(document: JsonObject, validateSectionReferences: Boolean = false): ResourceMusicalMetadata? {
    val value = document["musical"] ?: return null
    if (value is JsonNull) return null
    val musical = value as? JsonObject
        ?: throw IllegalArgumentException("Musical metadata must be an object.")
    require((musical["version"] as? JsonPrimitive)?.let { !it.isString && it.intOrNull == 1 } == true) {
        "Expected musical metadata version 1."
    }
    val role = musical.optionalText("role")
    require(role == null || role in setOf("exercise", "song", "fragment", "theory")) {
        "Musical role must be exercise, song, fragment or theory."
    }
    val difficulty = musical["difficulty"]?.takeUnless { it is JsonNull }?.let {
        val fields = it as? JsonObject
            ?: throw IllegalArgumentException("Musical difficulty must be an object.")
        val level = (fields["level"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
        require(level != null && level in 1..5) { "Musical difficulty level must be an integer from 1 to 5." }
        ResourceDifficulty(level, fields.textList("reasons"))
    }
    val conceptsValue = musical["concepts"]
    require(conceptsValue == null || conceptsValue is JsonArray) { "Musical concepts must be an array." }
    val concepts = (conceptsValue as? JsonArray).orEmpty().mapIndexed { index, item ->
        val fields = item as? JsonObject
            ?: throw IllegalArgumentException("Musical concept ${index + 1} must be an object.")
        val id = fields.optionalText("id")
        require(id != null && CONCEPT_ID.matches(id)) {
            "Musical concept ${index + 1} needs a namespaced id (for example chord:a-minor)."
        }
        val label = fields.optionalText("label")
        require(label != null) { "Musical concept $id needs a label." }
        val relation = fields.optionalText("relation")
        require(relation in setOf("trains", "uses", "explains")) {
            "Musical concept $id relation must be trains, uses or explains."
        }
        ResourceMusicalConcept(id, label, requireNotNull(relation), fields.textList("sectionIds"))
    }
    require(concepts.map { it.id to it.relation }.distinct().size == concepts.size) {
        "Musical concepts must not repeat the same id and relation; combine their sectionIds."
    }
    // References are optional. When supplied, they must target explicit, stable section ids.
    val sectionIds = (document["sections"] as? JsonArray).orEmpty().mapNotNull {
        ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.takeIf { id -> id.isString }?.content
    }
    if (validateSectionReferences) concepts.flatMap { it.sectionIds }.forEach { id ->
        require(sectionIds.count { it == id } == 1) { "Musical section reference '$id' must identify exactly one section." }
    }
    return ResourceMusicalMetadata(role, difficulty, concepts)
}

internal fun JsonObject.importedResourceTags(): List<String> = textList("tags").distinct()

private val CONCEPT_ID = Regex("(chord|transition|progression|scale|arpeggio|pattern|technique):[a-z0-9]+(?:[._-][a-z0-9]+)*")

private fun JsonObject.optionalText(key: String): String? {
    val value = this[key] ?: return null
    if (value is JsonNull) return null
    require(value is JsonPrimitive && value.isString && value.content.isNotBlank()) {
        "Musical field $key must be non-empty text."
    }
    return value.content
}

private fun JsonObject.textList(key: String): List<String> {
    val value: JsonElement = this[key] ?: return emptyList()
    require(value is JsonArray) { "Field $key must be an array of non-empty strings." }
    return value.map {
        require(it is JsonPrimitive && it.isString && it.content.isNotBlank()) {
            "Field $key must contain non-empty strings."
        }
        it.content
    }
}
