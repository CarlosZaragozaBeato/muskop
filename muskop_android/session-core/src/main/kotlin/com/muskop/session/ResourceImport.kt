package com.muskop.session

import java.util.Locale
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

const val MAX_RESOURCE_IMPORT_ITEMS = 100
const val MAX_RESOURCE_IMPORT_FILE_BYTES = 2_000_000
const val MAX_RESOURCE_IMPORT_BYTES = 10_000_000

data class ResourceImportSource(val name: String, val text: String)

/** Prepared content, never a set of edits to existing resource identities. */
class ResourceImportBatch internal constructor(internal val documents: List<JsonObject>) {
    val resources: List<SessionResource> get() = documents.mapIndexed { index, resource ->
        SessionResource(
            "import-preview-$index", resource.requiredText("title"), resource.requiredText("type"),
            (resource["category"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
            resource.importedResourceTags(), resource.getValue("content"), resource["exercise"] as? JsonObject,
            resource["learning"], resource["organization"],
        )
    }
}

fun prepareResourceImport(raw: String): ResourceImportBatch = prepareResourceImports(listOf(ResourceImportSource("JSON", raw)))

fun prepareResourceImports(sources: List<ResourceImportSource>): ResourceImportBatch {
    require(sources.isNotEmpty()) { "Choose at least one JSON file or paste resource JSON." }
    require(sources.size <= MAX_RESOURCE_IMPORT_ITEMS) { "Choose at most $MAX_RESOURCE_IMPORT_ITEMS files." }
    val sizes = sources.map { it.text.toByteArray(Charsets.UTF_8).size }
    sizes.forEachIndexed { index, bytes ->
        require(bytes <= MAX_RESOURCE_IMPORT_FILE_BYTES) { "${sources[index].name}: file is larger than 2 MB." }
    }
    require(sizes.sumOf { it.toLong() } <= MAX_RESOURCE_IMPORT_BYTES) { "The selected files exceed 10 MB in total." }
    val documents = mutableListOf<JsonObject>()
    sources.forEach { source ->
        try {
            val root = Json.parseToJsonElement(source.text.trimStart().removePrefix("\uFEFF"))
            val entries = when (root) {
                is JsonArray -> root
                is JsonObject -> {
                    require("muskopSession" !in root) { "Use Settings to import a session. This action adds resources only." }
                    if ("muskopResources" in root) {
                        require((root["muskopResources"] as? JsonPrimitive)?.let { !it.isString && it.intOrNull == 1 } == true) {
                            "Expected resource package version 1."
                        }
                        root["resources"] as? JsonArray ?: throw IllegalArgumentException("The package needs a resources array.")
                    } else JsonArray(listOf(root))
                }
                else -> throw IllegalArgumentException("Expected a resource, an array or a resource package.")
            }
            require(entries.isNotEmpty()) { "The resource selection is empty." }
            require(documents.size + entries.size <= MAX_RESOURCE_IMPORT_ITEMS) { "Import at most $MAX_RESOURCE_IMPORT_ITEMS resources at once." }
            entries.forEachIndexed { index, item ->
                try {
                    documents += normalizeImportedResource(item as? JsonObject
                        ?: throw IllegalArgumentException("A resource must be an object."))
                } catch (error: IllegalArgumentException) {
                    throw IllegalArgumentException("Resource ${index + 1}: ${error.message}", error)
                }
            }
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("${source.name}: ${error.message}", error)
        }
    }
    checkBatchSize(documents)
    return ResourceImportBatch(documents.toList())
}

/** Validate all entries first, then append everything with fresh ids in one session mutation. */
fun SessionDocument.importResources(batch: ResourceImportBatch): SessionDocument {
    require(batch.documents.isNotEmpty() && batch.documents.size <= MAX_RESOURCE_IMPORT_ITEMS) { "Import between 1 and $MAX_RESOURCE_IMPORT_ITEMS resources." }
    val prepared = batch.documents.map(::normalizeImportedResource)
    checkBatchSize(prepared)
    val existing = root["resources"] as? JsonArray ?: throw SessionFormatException("The resources field must be a JSON array")
    val ids = existing.mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.content }.toMutableSet()
    val additions = prepared.map { resource ->
        var id: String
        do { id = UUID.randomUUID().toString() } while (!ids.add(id))
        JsonObject(resource + ("id" to JsonPrimitive(id)))
    }
    return withRootValue("resources", JsonArray(existing + additions))
}

private fun normalizeImportedResource(resource: JsonObject): JsonObject {
    require(listOf("muskopSession", "muskopRoutine", "muskopExercisePack", "muskopResources").none { it in resource }) {
        "Expected a TAB or THEORY resource, not a session, routine or nested package."
    }
    if ("version" in resource && "sections" in resource) {
        val input = importTabResource(resource.toString())
        return JsonObject(mapOf(
            "title" to JsonPrimitive(input.title), "type" to JsonPrimitive("TAB"),
            "category" to (input.category?.let(::JsonPrimitive) ?: JsonNull),
            "tags" to JsonArray(input.tags.map(::JsonPrimitive)), "content" to input.content,
        ))
    }
    val title = resource.requiredText("title")
    val type = resource.requiredText("type").uppercase(Locale.ROOT)
    require(type in setOf("TAB", "THEORY")) { "Only TAB and THEORY resources are supported in this import." }
    val content = resource["content"] as? JsonObject ?: throw IllegalArgumentException("The resource needs object content.")
    val preparedContent = if (type == "TAB") {
        importTabResource(content.toString()).content
    } else {
        require((content["kind"] as? JsonPrimitive)?.content == "theory") { "Theory content needs kind theory." }
        require((content["body"] as? JsonPrimitive)?.isString == true) { "Theory content needs a text body." }
        readMusicalMetadata(content, validateSectionReferences = true)
        content
    }
    val category = resource["category"]
    require(category == null || category is JsonNull || (category is JsonPrimitive && category.isString)) { "Category must be text." }
    val tags = if ("tags" in resource) resource.importedResourceTags() else content.importedResourceTags()
    return JsonObject((resource - "id") + mapOf(
        "title" to JsonPrimitive(title), "type" to JsonPrimitive(type), "content" to preparedContent,
        "tags" to JsonArray(tags.map(::JsonPrimitive)),
    ))
}

private fun JsonObject.requiredText(key: String): String = (this[key] as? JsonPrimitive)
    ?.takeIf { it.isString && it.content.isNotBlank() }?.content
    ?: throw IllegalArgumentException("The resource needs a non-empty $key.")

private fun checkBatchSize(documents: List<JsonObject>) {
    require(documents.sumOf { it.toString().toByteArray(Charsets.UTF_8).size.toLong() } <= MAX_RESOURCE_IMPORT_BYTES) {
        "Prepared resources exceed 10 MB in total."
    }
}
