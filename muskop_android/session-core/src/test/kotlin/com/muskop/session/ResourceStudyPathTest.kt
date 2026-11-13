package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ResourceStudyPathTest {
    private val change = "transition:a-minor_to_c-major"
    private val pattern = "pattern:p-i-m-a"
    private fun resource(id: String, vararg concepts: Pair<String, String>): SessionResource {
        val musical = JsonObject(mapOf("version" to JsonPrimitive(1), "concepts" to JsonArray(concepts.map { (concept, relation) ->
            JsonObject(mapOf("id" to JsonPrimitive(concept), "label" to JsonPrimitive(concept), "relation" to JsonPrimitive(relation)))
        })))
        return SessionResource(id, id, "TAB", null, listOf("shared"),
            JsonObject(mapOf("version" to JsonPrimitive(2), "title" to JsonPrimitive(id), "sections" to JsonArray(emptyList()), "musical" to musical)), null)
    }

    @Test fun `path groups preparation and explanations for the applied concepts of its target`() {
        val target = resource("target", change to "uses", pattern to "uses", "technique:barre" to "trains")
        val exercise = resource("exercise", change to "trains", pattern to "trains")
        val explanation = resource("theory", change to "explains", "technique:barre" to "explains")
        val path = resourceStudyPath(target, listOf(target, exercise, explanation))
        assertEquals(listOf(change, pattern), path.requirements.map { it.id })
        assertEquals(listOf(exercise.id), path.preparation.map { it.resource.id })
        assertEquals(2, path.preparation.single().concepts.size)
        assertEquals(listOf(change), path.explanations.single().concepts.map { it.id })
        assertEquals(emptyList(), path.withoutPreparation)
        assertTrue(path.requirements.all { it.preparationResourceIds == listOf("exercise") })
    }

    @Test fun `shared tags labels applied concepts and theory never count as training`() {
        val target = resource("target", change to "uses")
        val anotherPiece = resource("piece", change to "uses")
        val theory = resource("theory", change to "explains")
        val differentChange = resource("unrelated", "transition:c-major_to_g-major" to "trains")
        val path = resourceStudyPath(target, listOf(anotherPiece, theory, differentChange))
        assertTrue(path.preparation.isEmpty())
        assertEquals(listOf(change), path.withoutPreparation.map { it.id })
        assertEquals(listOf("theory"), path.explanations.map { it.resource.id })
    }

    @Test fun `missing preparation stays explicit even when some target concepts are covered`() {
        val target = resource("target", change to "uses", pattern to "uses")
        val path = resourceStudyPath(target, listOf(resource("exercise", change to "trains")))
        assertEquals(listOf(pattern), path.withoutPreparation.map { it.id })
        assertEquals(listOf("exercise"), path.requirements.first().preparationResourceIds)
        assertTrue(path.requirements.last().preparationResourceIds.isEmpty())
    }

    @Test fun `alternatives are retained without duplicate cards or using the target to prepare itself`() {
        val target = resource("target", change to "uses", change to "trains")
        val a = resource("a", change to "trains")
        val b = resource("b", change to "trains")
        val path = resourceStudyPath(target, listOf(target, a, a, b))
        assertEquals(listOf("a", "b"), path.preparation.map { it.resource.id })
        assertEquals(1, path.requirements.size)
        assertEquals(listOf("a", "b"), path.requirements.single().preparationResourceIds)
    }

    @Test fun `legacy future and training-only resources have no invented application requirements`() {
        val legacy = resource("legacy").copy(content = JsonObject(emptyMap()))
        val future = resource("future", change to "uses").let { it.copy(content = Json.parseToJsonElement(it.content.toString().replace("\"version\":1", "\"version\":99"))) }
        val exercise = resource("exercise", change to "trains")
        listOf(legacy, future, exercise).forEach { target ->
            val path = resourceStudyPath(target, listOf(exercise))
            assertTrue(path.requirements.isEmpty())
            assertTrue(path.preparation.isEmpty())
            assertTrue(path.explanations.isEmpty())
        }
    }

    @Test fun `locations preserve missing references instead of inventing a section`() {
        val target = resource("target", change to "uses")
        val document = target.content as JsonObject
        val musical = document["musical"] as JsonObject
        val concept = (musical["concepts"] as JsonArray).single() as JsonObject
        val withReference = target.copy(content = JsonObject(document + ("musical" to JsonObject(musical +
            ("concepts" to JsonArray(listOf(JsonObject(concept + ("sectionIds" to JsonArray(listOf(JsonPrimitive("missing"))))))))))))
        val requirement = resourceStudyPath(withReference, emptyList()).requirements.single()
        assertEquals(listOf("missing"), requirement.targetLocation.unavailableSectionIds)
        assertTrue(requirement.targetLocation.sectionNames.isEmpty())
        assertEquals(false, requirement.targetLocation.wholeResource)
    }

    @Test fun `recalculation follows the library without changing data or interpreting learning as mastery`() {
        val target = resource("target", change to "uses")
        val exercise = resource("exercise", change to "trains")
        val session = listOf(target, exercise).fold(SessionFactory.create("Test")) { document, resource ->
            document.saveResource(ResourceEditorInput(resource.id, resource.title, resource.type, null, resource.tags, resource.content))
        }.setResourceLearningStatus("exercise", ResourceLearningStatus.MAINTAINING)
        val before = session.toJson()
        val path = resourceStudyPath(session.resources().first(), session.resources())
        assertEquals(ResourceLearningStatus.MAINTAINING, path.preparation.single().resource.learningStatus())
        assertEquals(before, session.toJson())
        val withoutExercise = resourceStudyPath(target, listOf(target))
        assertEquals(listOf(change), withoutExercise.withoutPreparation.map { it.id })
    }

    @Test fun `mutually applied and trained concepts stay a finite direct path`() {
        val target = resource("target", change to "uses", pattern to "trains")
        val exercise = resource("exercise", change to "trains", pattern to "uses")
        val path = resourceStudyPath(target, listOf(target, exercise))
        assertEquals(listOf(change), path.requirements.map { it.id })
        assertEquals(listOf("exercise"), path.preparation.map { it.resource.id })
        assertEquals(listOf(change), path.preparation.single().concepts.map { it.id })
    }
}
