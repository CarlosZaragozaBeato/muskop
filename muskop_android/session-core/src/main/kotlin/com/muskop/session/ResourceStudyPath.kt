package com.muskop.session

data class StudyPathRequirement(
    val id: String,
    val label: String,
    val targetLocation: ConceptLocation,
    val preparationResourceIds: List<String>,
)

/** A direct study sequence from declared concepts, not an assessment or prerequisite graph. */
data class ResourceStudyPath(
    val target: SessionResource,
    val requirements: List<StudyPathRequirement>,
    val preparation: List<ResourceConnection>,
    val explanations: List<ResourceConnection>,
) {
    val withoutPreparation: List<StudyPathRequirement>
        get() = requirements.filter { it.preparationResourceIds.isEmpty() }
}

fun resourceStudyPath(target: SessionResource, library: List<SessionResource>): ResourceStudyPath {
    val concepts = target.musicalMetadata()?.concepts.orEmpty().filter { it.relation == "uses" }
    val ids = concepts.map { it.id }.toSet()
    val connections = resourceConnections(target, library).mapNotNull { connection ->
        val relevant = connection.concepts.filter { it.id in ids }
        if (relevant.isEmpty()) null else connection.copy(concepts = relevant)
    }
    val preparation = connections.filter { it.kind == ResourceConnectionKind.PREPARATION }
    return ResourceStudyPath(
        target = target,
        requirements = concepts.map { concept ->
            StudyPathRequirement(concept.id, concept.label, conceptLocation(target, listOf(concept)),
                preparation.filter { connection -> connection.concepts.any { it.id == concept.id } }.map { it.resource.id })
        },
        preparation = preparation,
        explanations = connections.filter { it.kind == ResourceConnectionKind.EXPLANATION },
    )
}
