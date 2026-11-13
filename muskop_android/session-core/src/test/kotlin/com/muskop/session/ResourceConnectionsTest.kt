package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ResourceConnectionsTest {
    private fun resource(id: String, relation: String, concept: String = "transition:a-minor_to_c-major",
                         label: String = "Am → C", sectionIds: String = "[]", sections: String = "[]"): SessionResource {
        val content = Json.parseToJsonElement("""{"version":2,"title":"$id","sections":$sections,
          "musical":{"version":1,"concepts":[{"id":"$concept","label":"$label",
            "relation":"$relation","sectionIds":$sectionIds}]}}""")
        return SessionResource(id, id, "TAB", null, listOf("same-tag"), content, null)
    }

    @Test fun `preparation and usefulness are reciprocal and exclude self`() {
        val exercise = resource("exercise", "trains")
        val song = resource("song", "uses", label = "La menor a Do")
        val library = listOf(exercise, song)
        val preparation = resourceConnections(song, library).single()
        assertEquals(ResourceConnectionKind.PREPARATION, preparation.kind)
        assertEquals(exercise.id, preparation.resource.id)
        assertEquals("La menor a Do", preparation.concepts.single().label)
        val application = resourceConnections(exercise, library).single()
        assertEquals(ResourceConnectionKind.APPLICATION, application.kind)
        assertEquals(song.id, application.resource.id)
    }

    @Test fun `same tags names or uses do not imply preparation`() {
        val song = resource("song", "uses")
        val otherSong = resource("another song", "uses")
        val unrelated = resource("exercise", "trains", concept = "chord:a-minor")
        assertTrue(resourceConnections(song, listOf(song, otherSong, unrelated)).isEmpty())
    }

    @Test fun `theory explains both applied and trained concepts with reverse usefulness`() {
        val theory = resource("theory", "explains").copy(type = "THEORY")
        listOf(resource("exercise", "trains"), resource("song", "uses")).forEach { source ->
            assertEquals(ResourceConnectionKind.EXPLANATION, resourceConnections(source, listOf(theory)).single().kind)
            assertEquals(ResourceConnectionKind.APPLICATION, resourceConnections(theory, listOf(source)).single().kind)
        }
    }

    @Test fun `locations distinguish whole resource explicit sections missing and ambiguous references`() {
        val exercise = resource("exercise", "trains", sectionIds = """["study"]""",
            sections = """[{"id":"study","name":"Slow changes"}]""")
        val song = resource("song", "uses", sectionIds = """["verse","missing","duplicate"]""",
            sections = """[{"id":"verse","name":"Verse"},{"id":"duplicate"},{"id":"duplicate"}]""")
        val match = resourceConnections(song, listOf(exercise)).single().concepts.single()
        assertEquals(listOf("Verse"), match.sourceLocation.sectionNames)
        assertEquals(listOf("missing", "duplicate"), match.sourceLocation.unavailableSectionIds)
        assertEquals(false, match.sourceLocation.wholeResource)
        assertEquals(listOf("Slow changes"), match.targetLocation.sectionNames)
        val wholeSong = resource("whole", "uses")
        assertTrue(resourceConnections(wholeSong, listOf(exercise)).single().concepts.single().sourceLocation.wholeResource)
    }

    @Test fun `legacy future or malformed metadata does not produce edges or modify data`() {
        val exercise = resource("exercise", "trains")
        val song = resource("song", "uses")
        val original = song.content as JsonObject
        val metadata = original["musical"] as JsonObject
        val invalid = listOf(
            song.copy(content = JsonObject(original - "musical")),
            song.copy(content = JsonObject(original + ("musical" to JsonPrimitive("invalid")))),
            song.copy(content = JsonObject(original + ("musical" to JsonObject(metadata + ("version" to JsonPrimitive(99)))))),
        )
        invalid.forEach {
            assertTrue(resourceConnections(it, listOf(exercise)).isEmpty())
            assertTrue(resourceConnections(exercise, listOf(it)).isEmpty())
        }
        val session = SessionFactory.create("Test").saveResource(ResourceEditorInput(null, "Song", "TAB", null, emptyList(), song.content))
        val before = session.toJson()
        resourceConnections(session.resources().single(), listOf(exercise))
        assertEquals(before, session.toJson())
    }

    @Test fun `groups concepts per target sorts by count and removes duplicate targets`() {
        val exercise = resource("exercise", "trains")
        val song = resource("z-song", "uses")
        val secondConcept = Json.parseToJsonElement("""{"id":"pattern:p-i-m-a","label":"PIMA","relation":"uses"}""")
        fun addConcept(resource: SessionResource, concept: kotlinx.serialization.json.JsonElement): SessionResource {
            val doc = resource.content as JsonObject
            val metadata = doc["musical"] as JsonObject
            val concepts = metadata["concepts"] as JsonArray
            return resource.copy(content = JsonObject(doc + ("musical" to JsonObject(metadata + ("concepts" to JsonArray(concepts + concept))))))
        }
        val richSong = addConcept(song, secondConcept)
        val richExercise = addConcept(exercise, JsonObject((secondConcept as JsonObject) + ("relation" to JsonPrimitive("trains"))))
        val simpleSong = resource("a-song", "uses")
        val connections = resourceConnections(richExercise, listOf(simpleSong, richSong, richSong))
        assertEquals(listOf("z-song", "a-song"), connections.map { it.resource.id })
        assertEquals(2, connections.first().concepts.size)
        assertEquals(1, resourceConnections(richExercise, listOf(simpleSong)).size)
        assertTrue(resourceConnections(richExercise, emptyList()).isEmpty())
    }
}
