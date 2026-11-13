package com.muskop.session

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ResourceLearningTest {
    private val raw = """{
      "muskopSession":2,"user":{"username":"Test","createdAt":"2026-01-01T00:00:00Z","custom":true},
      "resources":[
        {"id":"exercise","title":"Study","type":"TAB","tags":["warmup"],"customResource":{"keep":true},
         "exercise":{"skill":"arpegios","level":3},
         "content":{"version":2,"title":"Study","sections":[],"customContent":true,
           "musical":{"version":1,"role":"exercise","difficulty":{"level":2},
             "concepts":[{"id":"pattern:p-i-m-a","label":"PIMA","relation":"trains"}]}}},
        {"id":"theory","title":"Theory","type":"THEORY","content":{"kind":"theory","body":"Read"},
         "learning":{"status":"maintaining","updatedAt":"2026-01-01T00:00:00Z","futureField":[1,2]}}
      ],
      "routines":[],"practiceLog":[],"experience":{"general":100},"customRoot":[1,2],
      "activePractice":{"kind":"free","routineId":"free","elapsedBySkillSeconds":{"general":15}}
    }"""

    private fun session() = SessionImporter.fromJson(raw)

    @Test fun `legacy resources are unassessed without mutating the document`() {
        val session = session()
        assertNull(session.resources().first().learningStatus())
        assertEquals(ResourceLearningStatus.MAINTAINING, session.resources().last().learningStatus())
        assertEquals(Json.parseToJsonElement(raw), Json.parseToJsonElement(session.toJson()))
        assertSame(session, session.setResourceLearningStatus("exercise", null))
    }

    @Test fun `all statuses persist through export reopen and only learning data changes`() {
        val original = session()
        ResourceLearningStatus.entries.forEach { status ->
            val updated = original.setResourceLearningStatus("exercise", status)
            val reopened = SessionImporter.fromJson(updated.toJson())
            assertEquals(2, reopened.version)
            assertEquals(status, reopened.resources().first().learningStatus())
            val learning = reopened.resources().first().learning as JsonObject
            assertEquals(status.value, learning["status"]?.jsonPrimitive?.content)
            Instant.parse(learning.getValue("updatedAt").jsonPrimitive.content)
            assertEquals(
                original.toJsonObject().filterKeys { it !in setOf("user", "resources") },
                updated.toJsonObject().filterKeys { it !in setOf("user", "resources") },
            )
            assertEquals(original.resources().first().content, reopened.resources().first().content)
            val oldResources = original.toJsonObject()["resources"] as JsonArray
            val newResources = updated.toJsonObject()["resources"] as JsonArray
            assertEquals(oldResources[1], newResources[1])
            assertEquals((oldResources[0] as JsonObject).filterKeys { it != "learning" },
                (newResources[0] as JsonObject).filterKeys { it != "learning" })
            assertEquals(original.toJsonObject().getValue("user").let { it as JsonObject }.filterKeys { it != "updatedAt" },
                updated.toJsonObject().getValue("user").let { it as JsonObject }.filterKeys { it != "updatedAt" })
        }
    }

    @Test fun `clearing a rating keeps unknown learning fields and leaves it unassessed`() {
        val cleared = session().setResourceLearningStatus("theory", null)
        val resource = SessionImporter.fromJson(cleared.toJson()).resources().last()
        assertNull(resource.learningStatus())
        val learning = resource.learning as JsonObject
        assertEquals(JsonNull, learning["status"])
        assertEquals(Json.parseToJsonElement("[1,2]"), learning["futureField"])
        assertSame(cleared, cleared.setResourceLearningStatus("theory", null))
    }

    @Test fun `saving the same rating is a no-op and does not refresh assessment time`() {
        val session = session()
        assertSame(session, session.setResourceLearningStatus("theory", ResourceLearningStatus.MAINTAINING))
    }

    @Test fun `future statuses remain opaque through general resource editing`() {
        val future = SessionImporter.fromJson(raw.replace("maintaining", "future-status"))
        val resource = future.resources().last()
        assertNull(resource.learningStatus())
        val edited = future.saveResource(ResourceEditorInput(resource.id, "Renamed", resource.type,
            resource.category, resource.tags, resource.content))
        val reopened = SessionImporter.fromJson(edited.toJson()).resources().last()
        assertEquals(resource.learning, reopened.learning)
        val changed = edited.setResourceLearningStatus(resource.id, ResourceLearningStatus.LEARNING).resources().last()
        assertEquals(ResourceLearningStatus.LEARNING, changed.learningStatus())
        assertEquals((resource.learning as JsonObject)["futureField"], (changed.learning as JsonObject)["futureField"])
    }

    @Test fun `missing ambiguous and blank resource identities fail without modifying the original`() {
        val session = session()
        val before = session.toJson()
        listOf("missing", "").forEach { id ->
            assertFailsWith<IllegalArgumentException> { session.setResourceLearningStatus(id, ResourceLearningStatus.LEARNING) }
        }
        val ambiguous = SessionImporter.fromJson(raw.replace("\"id\":\"theory\"", "\"id\":\"exercise\""))
        assertFailsWith<IllegalArgumentException> { ambiguous.setResourceLearningStatus("exercise", ResourceLearningStatus.LEARNING) }
        assertEquals(before, session.toJson())
    }

    @Test fun `practice and difficulty never change learning status or other resource ratings`() {
        val session = session().setResourceLearningStatus("exercise", ResourceLearningStatus.PLAYABLE)
        val after = session.recordPractice(PracticeEntryInput("2026-11-12", "free", "Free practice", 10, true, mapOf("arpegios" to 10)))
        assertEquals(session.resources(), after.resources())
        assertEquals(ResourceLearningStatus.PLAYABLE, after.resources().first().learningStatus())
        assertEquals(2, after.resources().first().musicalMetadata()?.difficulty?.level)
        assertEquals(ResourceLearningStatus.MAINTAINING, after.resources().last().learningStatus())
    }
}
