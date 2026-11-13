package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ResourcePracticeTest {
    private val first = "2026-11-12T10:00:01Z"
    private val last = "2026-11-12T10:01:01Z"
    private fun time() = PracticeTime().tick("general", "a", first).tick("arpegios", "b", last).tick("general", null)
    private fun entry(time: PracticeTime = time(), id: String = "practice-1") = PracticeEntryInput(
        "2026-11-12", "free", "Free practice", 1, false, mapOf("general" to 1),
        seconds = time.totalSeconds, id = id, resourceSeconds = time.resourceSeconds,
        resourceLastPracticedAt = time.resourceLastPracticedAt,
    )
    private fun snapshot(time: PracticeTime = time(), id: String = "practice-1") = ActivePracticeSnapshot(
        "free", "free", "Free practice", "general", 0, null, time.skillSeconds,
        practiceId = id, resourceSeconds = time.resourceSeconds, resourceLastPracticedAt = time.resourceLastPracticedAt,
        selectedResourceId = "b", showResource = false,
    )
    private fun session() = SessionFactory.create("Test")
        .saveResource(ResourceEditorInput("a", "A", "THEORY", null, emptyList(), theoryContent("A")))
        .saveResource(ResourceEditorInput("b", "B", "THEORY", null, emptyList(), theoryContent("B")))
        .setResourceLearningStatus("a", ResourceLearningStatus.PLAYABLE)
        .withRootValue("custom", JsonPrimitive("keep"))

    @Test fun `switching resources skills and unassigned practice partitions time without duplication`() {
        val time = time().tick("repertorio", "a", last)
        assertEquals(4, time.totalSeconds)
        assertEquals(mapOf("a" to 2, "b" to 1), time.resourceSeconds)
        assertEquals(mapOf("general" to 2, "arpegios" to 1, "repertorio" to 1), time.skillSeconds)
        assertEquals(last, time.resourceLastPracticedAt["a"])
        val saved = session().recordPractice(entry(time))
        assertEquals(ResourcePracticeSummary(2, last, 1), saved.resourcePracticeSummary("a"))
        assertEquals(ResourcePracticeSummary(1, last, 1), saved.resourcePracticeSummary("b"))
    }

    @Test fun `checkpoint resumes selection memory mode and previous resource time without recounting it`() {
        val original = session().saveActivePractice(snapshot())
        val active = original.toJsonObject()["activePractice"] as JsonObject
        val withFuture = original.withRootValue("activePractice", JsonObject(active + ("future" to JsonPrimitive("keep"))))
        val resumed = requireNotNull(SessionImporter.fromJson(withFuture.toJson()).activePractice())
        assertEquals("b", resumed.selectedResourceId)
        assertEquals(false, resumed.showResource)
        assertEquals("practice-1", resumed.practiceId)
        assertEquals(0L, withFuture.resourcePracticeSummary("a").totalSeconds)
        val next = PracticeTime(resumed.elapsedBySkillSeconds, resumed.resourceSeconds, resumed.resourceLastPracticedAt)
            .tick("general", "b", last)
        val checkpoint = withFuture.saveActivePractice(snapshot(next))
        assertEquals(JsonPrimitive("keep"), (checkpoint.toJsonObject()["activePractice"] as JsonObject)["future"])
        val saved = checkpoint.recordPractice(entry(next))
        assertNull(saved.activePractice())
        val reopened = SessionImporter.fromJson(saved.toJson())
        assertEquals(1L, reopened.resourcePracticeSummary("a").totalSeconds)
        assertEquals(2L, reopened.resourcePracticeSummary("b").totalSeconds)
        assertEquals(4, reopened.practiceEntries().single().seconds)
    }

    @Test fun `repeated saves and late autosaves never double count or resurrect a finished practice`() {
        val input = entry()
        val saved = session().saveActivePractice(snapshot()).recordPractice(input)
        assertSame(saved, saved.recordPractice(input))
        assertSame(saved, saved.saveActivePractice(snapshot()))
        val newerPending = saved.saveActivePractice(snapshot(PracticeTime(), "practice-2"))
        assertSame(newerPending, newerPending.recordPractice(input))
        assertEquals("practice-2", newerPending.activePractice()?.practiceId)
        assertEquals(1, newerPending.practiceEntries().size)
    }

    @Test fun `resource history is independent of current content learning and routine definitions`() {
        val original = session()
        val saved = original.recordPractice(entry())
        assertEquals(original.resources(), saved.resources())
        assertEquals(JsonPrimitive("keep"), saved.toJsonObject()["custom"])
        val modified = saved.saveResource(ResourceEditorInput("a", "Renamed", "THEORY", null, emptyList(), theoryContent("Changed")))
            .withRootValue("routines", JsonArray(emptyList()))
        assertEquals(saved.resourcePracticeSummary("a"), modified.resourcePracticeSummary("a"))
        assertEquals(ResourceLearningStatus.PLAYABLE, modified.resources().first().learningStatus())
        val second = modified.recordPractice(entry(PracticeTime().tick("general", "a", last), "practice-2"))
        assertEquals(ResourcePracticeSummary(2, last, 2), second.resourcePracticeSummary("a"))
    }

    @Test fun `legacy logs and checkpoints keep old time unassigned and preserve their unknown fields`() {
        val legacy = session().recordPractice(PracticeEntryInput("2026-11-11", "free", "Free", 10, false, emptyMap()))
        assertEquals(0L, legacy.resourcePracticeSummary("a").totalSeconds)
        val old = Json.parseToJsonElement("""{"kind":"free","routineId":"free","routineName":"Free",
          "blockIndex":0,"elapsedBySkillSeconds":{"general":100},"future":true}""")
        val pending = legacy.withRootValue("activePractice", old)
        val resumed = requireNotNull(pending.activePractice())
        val time = PracticeTime(resumed.elapsedBySkillSeconds, resumed.resourceSeconds, resumed.resourceLastPracticedAt)
            .tick("general", "a", last)
        val updated = pending.saveActivePractice(snapshot(time, resumed.practiceId))
        assertEquals(JsonPrimitive(true), (updated.toJsonObject()["activePractice"] as JsonObject)["future"])
        val saved = updated.recordPractice(entry(time, resumed.practiceId))
        assertEquals(101, saved.practiceEntries().last().seconds)
        assertEquals(1L, saved.resourcePracticeSummary("a").totalSeconds)
        assertNull(saved.activePractice())
        assertEquals(legacy.toJsonObject()["practiceLog"].let { it as JsonArray }[0],
            (saved.toJsonObject()["practiceLog"] as JsonArray)[0])
    }

    @Test fun `invalid attribution fails before changing a session`() {
        val original = session()
        val before = original.toJson()
        listOf(
            entry().copy(resourceSeconds = mapOf("a" to -1)),
            entry().copy(resourceSeconds = mapOf("a" to 10), resourceLastPracticedAt = mapOf("a" to last)),
            entry().copy(resourceLastPracticedAt = emptyMap()),
            entry().copy(resourceLastPracticedAt = mapOf("a" to "yesterday", "b" to last)),
            entry().copy(id = ""),
        ).forEach { assertFailsWith<IllegalArgumentException> { original.recordPractice(it) } }
        assertFailsWith<IllegalArgumentException> {
            original.saveActivePractice(snapshot().copy(resourceSeconds = mapOf("a" to 10)))
        }
        assertEquals(before, original.toJson())
    }

    @Test fun `last practiced is the latest actual resource tick even across midnight and out of order saves`() {
        val earlier = PracticeTime().tick("general", "a", "2026-11-11T23:59:59Z")
        val later = PracticeTime().tick("general", "a", "2026-11-12T00:00:01Z")
        val saved = session().recordPractice(entry(later, "later")).recordPractice(entry(earlier, "earlier"))
        assertEquals(ResourcePracticeSummary(2, "2026-11-12T00:00:01Z", 2), saved.resourcePracticeSummary("a"))
    }
}
