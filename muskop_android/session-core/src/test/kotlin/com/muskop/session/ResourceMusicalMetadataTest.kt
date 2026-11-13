package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ResourceMusicalMetadataTest {
    private val raw = """{
      "version":2,"title":"Am to C study","category":"fingerstyle",
      "tags":["warmup","fingerstyle"],"future":{"preserve":true},
      "musical":{
        "version":1,"role":"exercise","futureMusical":[1,2],
        "difficulty":{"level":2,"reasons":["Alternating bass"],"futureDifficulty":true},
        "concepts":[{"id":"transition:a-minor_to_c-major","label":"Am → C",
          "relation":"trains","sectionIds":["changes"],"futureConcept":"keep"}]
      },
      "sections":[{"id":"changes","kind":"fingerstyle","name":"Changes",
        "measures":[{"events":[{"beat":1,"notes":[{"string":6,"fret":0,"finger":"p","custom":true}]}]}]}]
    }"""

    @Test fun `import maps tags and preserves musical metadata through save edit export and reopen`() {
        val input = importTabResource(raw)
        assertEquals(listOf("warmup", "fingerstyle"), input.tags)
        assertEquals(Json.parseToJsonElement(raw), input.content)
        val session = SessionFactory.create("Test")
            .withRootValue("customSession", JsonPrimitive("keep"))
            .saveResource(input)
        val resource = session.resources().single()
        val metadata = requireNotNull(resource.musicalMetadata())
        assertEquals("exercise", metadata.role)
        assertEquals(ResourceDifficulty(2, listOf("Alternating bass")), metadata.difficulty)
        assertEquals(listOf("changes"), metadata.concepts.single().sectionIds)
        val edited = session.saveResource(input.copy(id = resource.id, title = "Renamed", tags = listOf("new")))
        val reopened = SessionImporter.fromJson(edited.toJson()).resources().single()
        assertEquals("Renamed", reopened.title)
        assertEquals(input.content, reopened.content)
        assertEquals(metadata, reopened.musicalMetadata())
        assertEquals(session.routines(), edited.routines())
        assertEquals(session.practiceEntries(), edited.practiceEntries())
        assertEquals(
            session.toJsonObject().filterKeys { it !in setOf("resources", "user") },
            edited.toJsonObject().filterKeys { it !in setOf("resources", "user") },
        )
    }

    @Test fun `exercise and song share a stable concept regardless of their displayed label`() {
        val session = SessionFactory.create("Test")
            .saveResource(importTabResource(raw))
            .saveResource(importTabResource(raw.replace("\"exercise\"", "\"song\"")
                .replace("\"trains\"", "\"uses\"").replace("Am → C", "Cambio de La menor a Do")))
        val concepts = session.resources().map { requireNotNull(it.musicalMetadata()).concepts.single() }
        assertEquals(concepts[0].id, concepts[1].id)
        assertEquals(listOf("trains", "uses"), concepts.map { it.relation })
    }

    @Test fun `legacy tabs need no metadata and retain generated section identities`() {
        val input = importTabResource("""{"version":2,"title":"Legacy","sections":[{"kind":"tab","measures":[]}]}""")
        val session = SessionFactory.create("Test").saveResource(input)
        val resource = session.resources().single()
        assertNull(resource.musicalMetadata())
        assertTrue(resource.tags.isEmpty())
        assertTrue(!resource.tabSections().single().id.isNullOrBlank())
        assertEquals(resource.content, SessionImporter.fromJson(session.toJson()).resources().single().content)
    }

    @Test fun `invalid imported metadata is rejected without changing the existing session`() {
        val session = SessionFactory.create("Test").saveResource(importTabResource(raw))
        val before = session.toJson()
        listOf(
            raw.replace("\"level\":2", "\"level\":0"),
            raw.replace("\"level\":2", "\"level\":6"),
            raw.replace("\"level\":2", "\"level\":\"2\""),
            raw.replace("\"level\":2", "\"level\":2.5"),
            raw.replace("\"role\":\"exercise\"", "\"role\":\"unknown\""),
            raw.replace("transition:a-minor_to_c-major", "Am C"),
            raw.replace("\"trains\"", "\"requires\""),
            raw.replace("\"sectionIds\":[\"changes\"]", "\"sectionIds\":[\"missing\"]"),
            raw.replace("\"concepts\":[", "\"concepts\":[null,"),
            raw.replace("\"label\":\"Am → C\"", "\"label\":\" \""),
            raw.replace("\"reasons\":[\"Alternating bass\"]", "\"reasons\":4"),
            raw.replace("\"sectionIds\":[\"changes\"]", "\"sectionIds\":\"changes\""),
            raw.replace("\"sections\":[", "\"sections\":[{\"id\":\"changes\"},"),
            raw.replace("\"tags\":[\"warmup\",\"fingerstyle\"]", "\"tags\":[1]"),
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { importTabResource(invalid) }
        }
        assertEquals(before, session.toJson())
    }

    @Test fun `future musical versions are rejected on resource import but preserved in session round trips`() {
        val document = Json.parseToJsonElement(raw) as JsonObject
        val musical = document["musical"] as JsonObject
        val future = JsonObject(document + ("musical" to JsonObject(musical + ("version" to JsonPrimitive(99)))))
        assertFailsWith<IllegalArgumentException> { importTabResource(future.toString()) }
        // Session loading never rebuilds or validates away opaque resource content.
        val session = SessionFactory.create("Test").saveResource(importTabResource(raw).copy(content = future))
        val reopened = SessionImporter.fromJson(session.toJson()).resources().single()
        assertEquals(future, reopened.content)
        assertNull(reopened.musicalMetadata())
    }

    @Test fun `editing theory body preserves imported musical and unknown fields`() {
        val content = Json.parseToJsonElement("""{"kind":"theory","body":"Before","custom":true,
          "musical":{"version":1,"role":"theory","concepts":[
            {"id":"technique:alternating-bass","label":"Bajo alternado","relation":"explains"}]}}""") as JsonObject
        val edited = theoryContent("After", content)
        assertEquals(content["musical"], edited["musical"])
        assertEquals(content["custom"], edited["custom"])
        assertEquals(JsonPrimitive("After"), edited["body"])
    }
}
