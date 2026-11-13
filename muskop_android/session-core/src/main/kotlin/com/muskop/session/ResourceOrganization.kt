package com.muskop.session

import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class ResourceStatus(val value: String) { ACTIVE("active"), HIDDEN("hidden"), COMPLETED("completed") }
enum class ResourcePriority(val value: String, val rank: Int) { HIGH("high", 2), NORMAL("normal", 1), LOW("low", 0) }
enum class ResourceView { ACTIVE, HIDDEN, COMPLETED, ALL }

fun SessionResource.organizationStatus(): ResourceStatus? {
    val raw = organization
    if (raw == null || raw is JsonNull) return ResourceStatus.ACTIVE
    val fields = raw as? JsonObject ?: return null
    val status = fields["status"]
    if (status == null || status is JsonNull) return ResourceStatus.ACTIVE
    return ResourceStatus.entries.firstOrNull { it.value == (status as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
}

fun SessionResource.resourcePriority(): ResourcePriority? {
    val raw = organization
    if (raw == null || raw is JsonNull) return ResourcePriority.NORMAL
    val fields = raw as? JsonObject ?: return null
    val priority = fields["priority"]
    if (priority == null || priority is JsonNull) return ResourcePriority.NORMAL
    return ResourcePriority.entries.firstOrNull { it.value == (priority as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
}

/** Stable sort preserves previous ordering when priorities match. Filtering never removes stored resources. */
fun List<SessionResource>.organizedResources(view: ResourceView = ResourceView.ACTIVE): List<SessionResource> =
    filter { resource -> when (view) {
        ResourceView.ACTIVE -> resource.organizationStatus() == ResourceStatus.ACTIVE
        ResourceView.HIDDEN -> resource.organizationStatus() == ResourceStatus.HIDDEN
        ResourceView.COMPLETED -> resource.organizationStatus() == ResourceStatus.COMPLETED
        ResourceView.ALL -> true
    } }.sortedByDescending { it.resourcePriority()?.rank ?: ResourcePriority.NORMAL.rank }

/** Null means leave that field untouched, including unsupported imported values. */
data class ResourceOrganizationUpdate(val status: ResourceStatus? = null, val priority: ResourcePriority? = null)

fun SessionDocument.updateResourceOrganization(resourceId: String, input: ResourceOrganizationUpdate): SessionDocument {
    require(resourceId.isNotBlank()) { "A resource id is required" }
    val resources = root["resources"] as? JsonArray ?: throw SessionFormatException("The resources field must be a JSON array")
    val matches = resources.mapIndexedNotNull { index, element ->
        val resource = element as? JsonObject ?: return@mapIndexedNotNull null
        if ((resource["id"] as? JsonPrimitive)?.content == resourceId) index to resource else null
    }
    require(matches.size == 1) { "The resource is missing or its id is ambiguous" }
    val (index, resource) = matches.single()
    val changes = buildMap<String, JsonElement> {
        input.status?.let { put("status", JsonPrimitive(it.value)) }
        input.priority?.let { put("priority", JsonPrimitive(it.value)) }
    }
    if (changes.isEmpty()) return this
    val fields = resource["organization"] as? JsonObject
    if (changes.all { fields?.get(it.key) == it.value }) return this
    val organization = JsonObject(fields.orEmpty() + changes + ("updatedAt" to JsonPrimitive(Instant.now().toString())))
    val updated = resources.toMutableList().apply { this[index] = JsonObject(resource + ("organization" to organization)) }
    return withRootValue("resources", JsonArray(updated))
}
