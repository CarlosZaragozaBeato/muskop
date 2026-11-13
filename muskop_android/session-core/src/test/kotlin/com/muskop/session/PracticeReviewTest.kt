package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class PracticeReviewTest {
    private fun session() = SessionFactory.create("Review")
        .saveResource(ResourceEditorInput("a", "Exercise", "THEORY", null, emptyList(), theoryContent("A")))
        .saveResource(ResourceEditorInput("b", "Song", "THEORY", null, emptyList(), theoryContent("B")))
        .withRootValue("future", JsonPrimitive("keep"))

    private fun entry(id: String = "one", day: String = "2026-11-13", seconds: Int = 45) = PracticeEntryInput(
        day, "free", "Free practice", 1, false, mapOf("general" to 1), seconds, id,
        mapOf("a" to 20, "b" to 10), mapOf("a" to "2026-11-13T09:00:20Z", "b" to "2026-11-13T09:00:30Z"),
    )

    @Test fun `free practice review partitions exact seconds and includes the saved practice in the day`() {
        val saved = session().recordPractice(entry())
        val before = saved.toJson()
        val review = assertNotNull(saved.practiceReview("one"))
        assertEquals(45, review.entry.seconds)
        assertEquals(listOf("a", "b"), review.resources.map { it.id })
        assertEquals(listOf(20, 10), review.resources.map { it.seconds })
        assertEquals(15L, review.unassignedSeconds)
        assertEquals(PracticeDaySummary("2026-11-13", 45L, 1, 2, 0), review.day)
        assertEquals(20L, review.resources.first().history.totalSeconds)
        assertNull(review.resources.first().resource?.masteryPercent())
        assertEquals(before, saved.toJson())
    }

    @Test fun `routine reviews use recorded time and completion even when routine definitions change`() {
        val partial = entry().copy(routineId = "routine", routineName = "Morning", completed = false)
        val complete = entry("two").copy(routineId = "routine", completed = true)
        val saved = session().recordPractice(partial).recordPractice(complete)
            .withRootValue("routines", JsonArray(emptyList()))
        assertEquals("Morning", saved.practiceReview("one")?.entry?.routineName)
        assertEquals(false, saved.practiceReview("one")?.entry?.completed)
        assertEquals(PracticeDaySummary("2026-11-13", 90L, 2, 2, 1), saved.practiceReview("two")?.day)
        assertEquals(40L, saved.practiceReview("two")?.resources?.first()?.history?.totalSeconds)
    }

    @Test fun `day uses log date isolates other days and keeps legacy time unassigned`() {
        val saved = session().recordPractice(entry(day = "2026-11-12"))
            .recordPractice(entry("today"))
            .recordPractice(PracticeEntryInput("2026-11-13", "free", null, 3, false, emptyMap(), id = "legacy"))
        assertEquals(PracticeDaySummary("2026-11-12", 45L, 1, 2, 0), saved.practiceReview("one")?.day)
        assertEquals(PracticeDaySummary("2026-11-13", 225L, 2, 2, 0), saved.practiceReview("today")?.day)
        val legacy = assertNotNull(saved.practiceReview("legacy"))
        assertEquals(emptyList(), legacy.resources)
        assertEquals(180L, legacy.unassignedSeconds)
        assertNull(saved.practiceReview("unknown"))
    }

    @Test fun `missing hidden and completed resources retain history and current assessment is editable independently`() {
        val saved = session().recordPractice(entry())
            .updateResourceOrganization("a", ResourceOrganizationUpdate(ResourceStatus.HIDDEN))
            .updateResourceOrganization("b", ResourceOrganizationUpdate(ResourceStatus.COMPLETED))
        val beforeLog = saved.toJsonObject()["practiceLog"]
        val updated = saved.updateResourceLearning("a", ResourceLearningUpdate(ResourceLearningStatus.LEARNING, 35))
        assertEquals(beforeLog, updated.toJsonObject()["practiceLog"])
        assertEquals(JsonPrimitive("keep"), updated.toJsonObject()["future"])
        val resource = assertNotNull(updated.practiceReview("one")?.resources?.first()?.resource)
        assertEquals(35, resource.masteryPercent())
        assertEquals(ResourceLearningStatus.LEARNING, resource.learningStatus())
        assertEquals(ResourceStatus.HIDDEN, resource.organizationStatus())
        val removed = updated.withRootValue("resources", JsonArray(emptyList()))
        val missing = assertNotNull(removed.practiceReview("one"))
        assertEquals(2, missing.resources.size)
        assertNull(missing.resources.first().resource)
        assertEquals(20, missing.resources.first().seconds)
    }

    @Test fun `retry reopening and duplicate ids never count the same practice twice`() {
        val original = session().recordPractice(entry())
        val log = original.toJsonObject()["practiceLog"] as JsonArray
        val duplicate = original.withRootValue("practiceLog", JsonArray(log + log))
        val reopened = SessionImporter.fromJson(duplicate.recordPractice(entry()).toJson())
        assertEquals(PracticeDaySummary("2026-11-13", 45L, 1, 2, 0), reopened.practiceReview("one")?.day)
        assertEquals(1, reopened.practiceReview("one")?.resources?.first()?.history?.practiceCount)
    }

    @Test fun `review draft and return checkpoint survive saved state serialization with the same identity and exact times`() {
        val input = entry()
        val checkpoint = ActivePracticeSnapshot(
            "free", "free", "Free practice", "general", 0, null, mapOf("general" to 45),
            "one", input.resourceSeconds, input.resourceLastPracticedAt, "b", false,
        )
        val bytes = java.io.ByteArrayOutputStream().also { output ->
            java.io.ObjectOutputStream(output).use { it.writeObject(listOf(input, checkpoint)) }
        }.toByteArray()
        val restored = java.io.ObjectInputStream(bytes.inputStream()).use { it.readObject() as List<*> }
        assertEquals(input, restored[0])
        assertEquals(checkpoint, restored[1])
        val pending = session().saveActivePractice(restored[1] as ActivePracticeSnapshot)
        assertEquals(45, pending.activePractice()?.elapsedBySkillSeconds?.get("general"))
        val saved = pending.recordPractice(restored[0] as PracticeEntryInput)
        assertNull(saved.activePractice())
        assertEquals(45, saved.practiceReview("one")?.entry?.seconds)
    }
}
