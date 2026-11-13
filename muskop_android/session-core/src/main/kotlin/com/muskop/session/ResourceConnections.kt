package com.muskop.session

import java.util.Locale
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class ResourceConnectionKind { PREPARATION, APPLICATION, EXPLANATION }

data class ConceptLocation(
    val wholeResource: Boolean,
    val sectionNames: List<String>,
    val unavailableSectionIds: List<String>,
)

data class SharedMusicalConcept(
    val id: String,
    val label: String,
    val sourceLocation: ConceptLocation,
    val targetLocation: ConceptLocation,
)

data class ResourceConnection(
    val resource: SessionResource,
    val kind: ResourceConnectionKind,
    val concepts: List<SharedMusicalConcept>,
)

/** Derived from the current library; never persists edges, scores or inferred mastery. */
fun resourceConnections(source: SessionResource, library: List<SessionResource>): List<ResourceConnection> {
    val sourceConcepts = source.musicalMetadata()?.concepts.orEmpty()
    if (sourceConcepts.isEmpty()) return emptyList()
    return library.filter { it.id != source.id }.distinctBy { it.id }.flatMap { target ->
        val targetConcepts = target.musicalMetadata()?.concepts.orEmpty()
        ResourceConnectionKind.entries.mapNotNull { kind ->
            val pairs = sourceConcepts.flatMap { from ->
                targetConcepts.filter { to ->
                    from.id == to.id && when (kind) {
                        ResourceConnectionKind.PREPARATION -> from.relation == "uses" && to.relation == "trains"
                        ResourceConnectionKind.APPLICATION ->
                            (from.relation == "trains" && to.relation == "uses") ||
                                (from.relation == "explains" && to.relation in setOf("uses", "trains"))
                        ResourceConnectionKind.EXPLANATION -> from.relation in setOf("uses", "trains") && to.relation == "explains"
                    }
                }.map { from to it }
            }
            if (pairs.isEmpty()) null else ResourceConnection(
                target, kind,
                pairs.groupBy { it.first.id }.map { (id, matches) ->
                    SharedMusicalConcept(
                        id, matches.first().first.label,
                        conceptLocation(source, matches.map { it.first }),
                        conceptLocation(target, matches.map { it.second }),
                    )
                },
            )
        }
    }.sortedWith(compareBy<ResourceConnection> { it.kind.ordinal }
        .thenByDescending { it.concepts.size }
        .thenBy { it.resource.title.lowercase(Locale.ROOT) }
        .thenBy { it.resource.id })
}

internal fun conceptLocation(resource: SessionResource, concepts: List<ResourceMusicalConcept>): ConceptLocation {
    val sections = ((resource.content as? JsonObject)?.get("sections") as? JsonArray).orEmpty()
        .mapNotNull { it as? JsonObject }
    fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    val names = mutableListOf<String>()
    val unavailable = mutableListOf<String>()
    concepts.flatMap { it.sectionIds }.distinct().forEach { id ->
        val section = sections.filter { it.text("id") == id }.singleOrNull()
        if (section == null) unavailable += id
        else names += section.text("name")?.takeIf { it.isNotBlank() } ?: "Unnamed section"
    }
    return ConceptLocation(concepts.any { it.sectionIds.isEmpty() }, names.distinct(), unavailable)
}
