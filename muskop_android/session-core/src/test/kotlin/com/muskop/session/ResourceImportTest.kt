package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class ResourceImportTest {
    @Test fun `documented package imports a connected exercise and theory without personal assessments`() {
        val raw = requireNotNull(javaClass.getResource("/fixtures/resource-bundle-v1.json")).readText()
        val batch = prepareResourceImport(raw)
        val imported = SessionFactory.create("Demo").importResources(batch)
        assertEquals(listOf("TAB", "THEORY"), imported.resources().map { it.type })
        assertEquals(ResourceConnectionKind.EXPLANATION, resourceConnections(imported.resources().first(), imported.resources()).single().kind)
        assertTrue(imported.resources().all { it.learning == null && it.organization == null })
    }
    private fun tab(title: String = "Study", relation: String = "trains") = """{
      "version":2,"title":"$title","category":"fingerstyle","tags":["warmup"],"custom":{"keep":true},
      "musical":{"version":1,"concepts":[{"id":"pattern:p-i-m-a","label":"PIMA","relation":"$relation","sectionIds":["part-a"]}]},
      "sections":[{"id":"part-a","kind":"fingerstyle","name":"A","measures":[{"events":[
        {"beat":1,"duration":"quarter","notes":[{"string":5,"fret":0,"finger":"p","technique":"custom","future":1}]}]}]}]
    }"""
    private fun existing() = SessionFactory.create("Test")
        .saveResource(ResourceEditorInput("existing", "Existing", "THEORY", null, emptyList(), theoryContent("Keep")))
        .recordPractice(PracticeEntryInput("2026-11-12", "free", "Free", 1, false, emptyMap()))
        .saveActivePractice(ActivePracticeSnapshot("free", "free", "Free", "general", 0, null, mapOf("general" to 5)))

    @Test fun `multiple files are added together with new identities and musical relationships`() {
        val batch = prepareResourceImports(listOf(ResourceImportSource("exercise.json", tab()), ResourceImportSource("song.json", tab("Song", "uses"))))
        val original = existing()
        val updated = original.importResources(batch)
        assertEquals(listOf("Existing", "Study", "Song"), updated.resources().map { it.title })
        assertEquals(listOf("warmup"), updated.resources()[1].tags)
        val song = updated.resources().last()
        assertEquals("Study", resourceConnections(song, updated.resources()).single().resource.title)
        assertEquals(original.toJsonObject().filterKeys { it !in setOf("resources", "user") },
            updated.toJsonObject().filterKeys { it !in setOf("resources", "user") })
        assertEquals(original.resources().single(), updated.resources().first())
        assertEquals(updated.toJsonObject(), SessionImporter.fromJson(updated.toJson()).toJsonObject())
    }

    @Test fun `versioned bundle supports theory and preserves unknown resource content and personal fields`() {
        val raw = """{"muskopResources":1,"resources":[{
          "id":"existing","title":"Imported theory","type":"THEORY","category":null,"tags":["theory"],
          "exercise":{"skill":"general","level":2},"future":{"keep":true},
          "learning":{"status":"learning","masteryPercent":40},
          "organization":{"status":"hidden","priority":"high"},
          "content":{"kind":"theory","body":"Read","custom":{"keep":true},"musical":{"version":1,
            "concepts":[{"id":"pattern:p-i-m-a","label":"PIMA","relation":"explains"}]}}
        }]}"""
        val originalResource = ((Json.parseToJsonElement(raw) as JsonObject)["resources"] as JsonArray).single() as JsonObject
        val batch = prepareResourceImport(raw)
        assertEquals(ResourceStatus.HIDDEN, batch.resources.single().organizationStatus())
        val updated = existing().importResources(batch)
        val imported = (updated.toJsonObject()["resources"] as JsonArray).last() as JsonObject
        assertNotEquals(originalResource["id"], imported["id"])
        assertEquals(originalResource - "id", imported - "id")
        assertEquals(40, updated.resources().last().masteryPercent())
        assertEquals(ResourceStatus.HIDDEN, updated.resources().last().organizationStatus())
    }

    @Test fun `array paste and single documents work including BOM and mixed inputs`() {
        val batch = prepareResourceImport("[${tab()}, {\"title\":\"Theory\",\"type\":\"THEORY\",\"content\":{\"kind\":\"theory\",\"body\":\"\"}}]")
        assertEquals(listOf("TAB", "THEORY"), batch.resources.map { it.type })
        assertEquals(1, prepareResourceImport("\uFEFF${tab()}").resources.size)
        assertEquals(Json.parseToJsonElement(tab()), batch.resources.first().content)
    }

    @Test fun `one invalid file or item rejects the entire selection with its location`() {
        val original = existing()
        val before = original.toJson()
        val error = assertFailsWith<IllegalArgumentException> {
            prepareResourceImports(listOf(ResourceImportSource("good.json", tab()), ResourceImportSource("bad.json", "[${tab()}, 3]")))
        }
        assertTrue(error.message.orEmpty().contains("bad.json: Resource 2"))
        val tampered = ResourceImportBatch(listOf(Json.parseToJsonElement("""{"title":"Bad","type":"THEORY","content":{}}""") as JsonObject))
        assertFailsWith<IllegalArgumentException> { original.importResources(tampered) }
        assertEquals(before, original.toJson())
    }

    @Test fun `empty future packages sessions and unsupported resource types are rejected`() {
        listOf("[]", "{}", "null", "{", """{"muskopResources":2,"resources":[]}""",
            """{"muskopResources":"1","resources":[]}""", """{"muskopResources":1,"resources":[]}""",
            """{"muskopSession":2,"resources":[]}""", """{"muskopRoutine":1,"name":"Routine"}""",
            """{"title":"Media","type":"MEDIA","content":{}}""").forEach {
            assertFailsWith<IllegalArgumentException> { prepareResourceImport(it) }
        }
    }

    @Test fun `duplicate titles and incoming ids never overwrite existing resources`() {
        val raw = """{"title":"Existing","type":"TAB","id":"existing","content":${tab()}}"""
        val batch = prepareResourceImport("[$raw,$raw]")
        val original = existing()
        val imported = original.importResources(batch)
        assertEquals(3, imported.resources().size)
        assertEquals(original.resources().first(), imported.resources().first())
        assertEquals(3, imported.resources().map { it.id }.toSet().size)
        val repeated = imported.importResources(batch)
        assertEquals(5, repeated.resources().map { it.id }.toSet().size)
    }

    @Test fun `metadata validation and canonical tags apply to every tab in the batch`() {
        val wrapped = """{"title":"Wrapped","type":"TAB","tags":["own"],"content":${tab()}}"""
        assertEquals(listOf("own"), prepareResourceImport(wrapped).resources.single().tags)
        assertFailsWith<IllegalArgumentException> { prepareResourceImport("[${tab()},${tab().replace("\"part-a\"]", "\"missing\"]")}]") }
        assertFailsWith<IllegalArgumentException> { prepareResourceImport(tab().replace("\"version\":1", "\"version\":99")) }
    }

    @Test fun `size and count limits reject oversized selections before parsing or saving`() {
        assertFailsWith<IllegalArgumentException> { prepareResourceImport("[" + List(101) { tab() }.joinToString() + "]") }
        assertFailsWith<IllegalArgumentException> { prepareResourceImport(" ".repeat(MAX_RESOURCE_IMPORT_FILE_BYTES + 1)) }
        val text = " ".repeat(1_800_000) + tab()
        assertFailsWith<IllegalArgumentException> { prepareResourceImports(List(6) { ResourceImportSource("$it.json", text) }) }
        assertFailsWith<IllegalArgumentException> { prepareResourceImports(emptyList()) }
    }
}
