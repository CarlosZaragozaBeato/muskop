package com.muskop.session

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SessionMigrationTest {
    private val json = Json { explicitNulls = true }

    @Test
    fun `migrating v1 retains every field except its version`() {
        val original = json.parseToJsonElement(fixture("session-v1-current.json")).jsonObject

        val migrated = SessionImporter.fromObject(original).toJsonObject()

        assertEquals(CURRENT_SESSION_VERSION, migrated.getValue("muskopSession").jsonPrimitive.int)
        assertEquals(original.keys, migrated.keys)
        original.forEach { (key, value) ->
            if (key != "muskopSession") assertEquals(value, migrated[key], "Se alteró $key")
        }
        assertEquals(1, migrated.getValue("projects").jsonArray.size)
        assertEquals(1, migrated.getValue("trash").jsonArray.size)
    }

    @Test
    fun `v2 export and import preserve the migrated document`() {
        val migrated = SessionImporter.fromJson(fixture("session-v1-current.json"))

        val reopened = SessionImporter.fromJson(migrated.toJson())

        assertEquals(migrated.toJsonObject(), reopened.toJsonObject())
    }

    @Test
    fun `atomic export can be read back`() {
        val destination = createTempDirectory("muskop-session-test").resolve("Carlos.muskop.json")
        val session = SessionImporter.fromJson(fixture("session-v1-current.json"))

        SessionFileStore.writeAtomically(destination, session)

        assertEquals(session.toJsonObject(), SessionFileStore.read(destination).toJsonObject())
    }

    @Test
    fun `future versions are rejected before they can be overwritten`() {
        val future = JsonObject(
            mapOf(
                "muskopSession" to JsonPrimitive(99),
                "user" to JsonObject(mapOf("username" to JsonPrimitive("Carlos"))),
                "resources" to kotlinx.serialization.json.JsonArray(emptyList()),
                "routines" to kotlinx.serialization.json.JsonArray(emptyList()),
                "practiceLog" to kotlinx.serialization.json.JsonArray(emptyList()),
            ),
        )

        assertFailsWith<SessionFormatException> {
            SessionImporter.fromObject(future)
        }
    }

    @Test
    fun `resources expose tags and tab sections without mutating the session`() {
        val resource = SessionImporter.fromJson(fixture("session-v1-current.json")).resources().single()

        assertEquals(listOf("bloque-5", "maj7"), resource.tags)
        assertEquals("Royal Road", resource.title)
        assertEquals("Subida", resource.tabSections().single().name)
    }

    @Test
    fun `practice records rich free practice entries while retaining session data`() {
        val session = SessionImporter.fromJson(fixture("session-v1-current.json"))

        val updated = session.recordPractice(
            PracticeEntryInput(
                date = "2026-11-12",
                routineId = "free",
                routineName = "Free practice",
                minutes = 12,
                completed = false,
                skillMinutes = mapOf("cambios-acordes" to 12),
            ),
        )

        assertEquals(session.resources(), updated.resources())
        assertEquals(session.routines(), updated.routines())
        val entry = updated.toJsonObject().getValue("practiceLog").jsonArray.last().jsonObject
        assertEquals("Free practice", entry.getValue("routineName").jsonPrimitive.content)
        assertEquals(12, entry.getValue("skillMinutes").jsonObject.getValue("cambios-acordes").jsonPrimitive.int)
    }

    @Test
    fun `progress reads legacy and enriched practice entries`() {
        val session = SessionImporter.fromJson(fixture("session-v1-current.json"))

        val entry = session.practiceEntries().single()

        assertEquals("Free practice", entry.routineName)
        assertEquals(10, entry.skillMinutes.getValue("cambios-acordes"))
        assertEquals(700, session.practiceGoals().weekly)
        assertEquals(500, session.skillGoals().getValue("cambios-acordes"))
    }

    @Test
    fun `active practice survives a session update and can be cleared`() {
        val session = SessionImporter.fromJson(fixture("session-v1-current.json"))
        val snapshot = ActivePracticeSnapshot(
            kind = "routine",
            routineId = "routine-1",
            routineName = "Royal Road",
            skill = null,
            blockIndex = 0,
            secondsLeft = 515,
            elapsedBySkillSeconds = mapOf("cambios-acordes" to 85),
        )

        val resumed = session.saveActivePractice(snapshot).activePractice()

        assertEquals(snapshot, resumed)
        assertEquals(null, session.saveActivePractice(snapshot).clearActivePractice().activePractice())
    }

    @Test
    fun `routine edits retain block fields that the editor does not own`() {
        val session = SessionImporter.fromJson(fixture("session-v1-current.json"))

        val updated = session.saveRoutine(
            RoutineEditorInput(
                id = "routine-1",
                name = "Royal Road actualizada",
                description = null,
                category = "cambios-acordes",
                blocks = listOf(
                    RoutineBlockInput("block-1", "Reto limpio", 12, 90, "resource-1", "cambios-acordes", "Sin tensión"),
                ),
            ),
        )

        val block = updated.toJsonObject().getValue("routines").jsonArray.single().jsonObject
            .getValue("blocks").jsonArray.single().jsonObject
        assertEquals(4, block.getValue("beatsPerMeasure").jsonPrimitive.int)
        assertEquals(4, block.getValue("subdivision").jsonPrimitive.int)
        assertEquals("Reto limpio", block.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun `resource metadata edits keep tab content intact`() {
        val session = SessionImporter.fromJson(fixture("session-v1-current.json"))
        val originalContent = session.resources().single().content

        val updated = session.saveResource(
            ResourceEditorInput("resource-1", "Royal Road final", "TAB", "repertorio", listOf("repaso"), originalContent),
        ).resources().single()

        assertEquals(originalContent, updated.content)
        assertEquals(listOf("repaso"), updated.tags)
    }

    @Test
    fun `tab section edits preserve existing measures`() {
        val content = SessionImporter.fromJson(fixture("session-v1-current.json")).resources().single().content
        val updated = updateTabSections(content, listOf(TabSectionInput("section-1", "fingerstyle", "Subida lenta", 50, null)))

        val measures = (updated as JsonObject).getValue("sections").jsonArray.single().jsonObject.getValue("measures").jsonArray
        assertEquals(1, measures.size)
    }

    @Test
    fun `optional external fixture is migrated without data loss`() {
        val fixturePath = System.getProperty("muskop.sessionFixture").orEmpty()
        if (fixturePath.isBlank()) return

        val original = json.parseToJsonElement(Files.readString(java.nio.file.Path.of(fixturePath))).jsonObject
        val migrated = SessionImporter.fromObject(original).toJsonObject()

        assertEquals(original.keys, migrated.keys)
        original.forEach { (key, value) ->
            if (key != "muskopSession") assertEquals(value, migrated[key], "Se alteró $key")
        }
        assertTrue(migrated.getValue("muskopSession").jsonPrimitive.int == CURRENT_SESSION_VERSION)
    }

    private fun fixture(name: String): String = requireNotNull(
        javaClass.getResource("/fixtures/$name"),
    ).readText()
}

