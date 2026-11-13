package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ResourceMasteryTest {
    private fun session() = SessionFactory.create("Test")
        .saveResource(ResourceEditorInput("study", "Study", "THEORY", null, listOf("keep"), theoryContent("Notes")))
        .setResourceLearningStatus("study", ResourceLearningStatus.LEARNING)

    @Test fun `missing mastery is not zero and legacy sessions remain unchanged`() {
        val session = session()
        assertNull(session.resources().single().masteryPercent())
        assertSame(session, session.updateResourceLearning("study", ResourceLearningUpdate(null, null, updateStatus = false)))
    }

    @Test fun `zero intermediate and full percentages round trip independently of learning status`() {
        val original = session()
        listOf(0, 37, 100).forEach { percent ->
            val changed = original.updateResourceLearning("study", ResourceLearningUpdate(null, percent, updateStatus = false))
            val reopened = SessionImporter.fromJson(changed.toJson())
            val resource = reopened.resources().single()
            assertEquals(percent, resource.masteryPercent())
            assertEquals(ResourceLearningStatus.LEARNING, resource.learningStatus())
            assertEquals(original.resources().single().content, resource.content)
            assertEquals(original.resources().single().tags, resource.tags)
            assertEquals(original.practiceEntries(), reopened.practiceEntries())
        }
    }

    @Test fun `percentage and status may be cleared independently or saved together`() {
        val assessed = session().updateResourceLearning("study", ResourceLearningUpdate(ResourceLearningStatus.PLAYABLE, 75))
        assertEquals(ResourceLearningStatus.PLAYABLE, assessed.resources().single().learningStatus())
        val clearedStatus = assessed.setResourceLearningStatus("study", null)
        assertEquals(75, clearedStatus.resources().single().masteryPercent())
        assertNull(clearedStatus.resources().single().learningStatus())
        val clearedMastery = assessed.updateResourceLearning("study", ResourceLearningUpdate(null, null, updateStatus = false))
        assertNull(clearedMastery.resources().single().masteryPercent())
        assertEquals(ResourceLearningStatus.PLAYABLE, clearedMastery.resources().single().learningStatus())
    }

    @Test fun `out of range percentages fail without modifying the session`() {
        val original = session()
        val before = original.toJson()
        listOf(-1, 101).forEach { percent ->
            assertFailsWith<IllegalArgumentException> {
                original.updateResourceLearning("study", ResourceLearningUpdate(ResourceLearningStatus.MAINTAINING, percent))
            }
        }
        assertEquals(before, original.toJson())
    }

    @Test fun `mastery edits preserve unsupported status and unknown learning fields`() {
        val raw = session().toJson().replace("\"status\": \"learning\"", "\"status\": \"future-status\", \"future\": {\"keep\": true}")
        val original = SessionImporter.fromJson(raw)
        assertNull(original.resources().single().learningStatus())
        val updated = original.updateResourceLearning("study", ResourceLearningUpdate(null, 62, updateStatus = false))
        val before = original.resources().single().learning as JsonObject
        val after = updated.resources().single().learning as JsonObject
        assertEquals(JsonPrimitive("future-status"), after["status"])
        assertEquals(before["future"], after["future"])
        assertEquals(62, updated.resources().single().masteryPercent())
    }

    @Test fun `unsupported imported mastery stays opaque during status or title editing`() {
        listOf("\"60\"", "60.5", "101", "true").forEach { unsupported ->
            val original = SessionImporter.fromJson(session().toJson().replace("\"status\": \"learning\"", "\"status\": \"learning\", \"masteryPercent\": $unsupported"))
            assertNull(original.resources().single().masteryPercent())
            val statusChanged = original.setResourceLearningStatus("study", ResourceLearningStatus.PLAYABLE)
            val old = original.resources().single()
            val renamed = statusChanged.saveResource(ResourceEditorInput(old.id, "Renamed", old.type, old.category, old.tags, old.content))
            assertEquals(Json.parseToJsonElement(unsupported), (renamed.resources().single().learning as JsonObject)["masteryPercent"])
        }
    }

    @Test fun `unchanged values are no ops and practice never changes mastery`() {
        val assessed = session().updateResourceLearning("study", ResourceLearningUpdate(ResourceLearningStatus.LEARNING, 40))
        assertSame(assessed, assessed.updateResourceLearning("study", ResourceLearningUpdate(ResourceLearningStatus.LEARNING, 40)))
        assertSame(assessed, assessed.setResourceLearningStatus("study", ResourceLearningStatus.LEARNING))
        val practiced = assessed.recordPractice(PracticeEntryInput("2026-11-12", "free", "Free", 1, false, mapOf("general" to 1)))
        assertEquals(assessed.resources(), practiced.resources())
    }
}
