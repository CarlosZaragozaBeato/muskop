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

class ResourceOrganizationTest {
    private fun session(): SessionDocument = listOf("a", "b", "c", "d").fold(SessionFactory.create("Test")) { doc, id ->
        doc.saveResource(ResourceEditorInput(id, id.uppercase(), "THEORY", null, listOf("keep"), theoryContent("Notes")))
    }.withRootValue("custom", JsonPrimitive("keep"))

    @Test fun `legacy resources are active with normal priority and retain their order`() {
        val session = session()
        val before = session.toJson()
        assertEquals(listOf("a", "b", "c", "d"), session.resources().organizedResources().map { it.id })
        session.resources().forEach {
            assertEquals(ResourceStatus.ACTIVE, it.organizationStatus())
            assertEquals(ResourcePriority.NORMAL, it.resourcePriority())
        }
        assertEquals(before, session.toJson())
    }

    @Test fun `hidden and completed are excluded by default but recoverable in explicit views`() {
        val changed = session()
            .updateResourceOrganization("a", ResourceOrganizationUpdate(ResourceStatus.HIDDEN, ResourcePriority.HIGH))
            .updateResourceOrganization("b", ResourceOrganizationUpdate(ResourceStatus.COMPLETED))
        assertEquals(listOf("c", "d"), changed.resources().organizedResources().map { it.id })
        assertEquals(listOf("a"), changed.resources().organizedResources(ResourceView.HIDDEN).map { it.id })
        assertEquals(listOf("b"), changed.resources().organizedResources(ResourceView.COMPLETED).map { it.id })
        assertEquals(4, changed.resources().organizedResources(ResourceView.ALL).size)
        val restored = changed.updateResourceOrganization("a", ResourceOrganizationUpdate(status = ResourceStatus.ACTIVE))
        assertEquals(listOf("a", "c", "d"), restored.resources().organizedResources().map { it.id })
    }

    @Test fun `priority sorts active resources first with stable ties and independent visibility`() {
        val changed = session()
            .updateResourceOrganization("a", ResourceOrganizationUpdate(priority = ResourcePriority.LOW))
            .updateResourceOrganization("c", ResourceOrganizationUpdate(priority = ResourcePriority.HIGH))
            .updateResourceOrganization("d", ResourceOrganizationUpdate(priority = ResourcePriority.HIGH))
        assertEquals(listOf("c", "d", "b", "a"), changed.resources().organizedResources().map { it.id })
        val hidden = changed.updateResourceOrganization("d", ResourceOrganizationUpdate(status = ResourceStatus.HIDDEN))
        assertEquals(listOf("c", "b", "a"), hidden.resources().organizedResources().map { it.id })
        assertEquals(ResourcePriority.HIGH, hidden.resources().last().resourcePriority())
    }

    @Test fun `all states and priorities survive export reopen and ordinary resource edits`() {
        ResourceStatus.entries.forEach { status -> ResourcePriority.entries.forEach { priority ->
            val changed = session().updateResourceOrganization("a", ResourceOrganizationUpdate(status, priority))
            val a = changed.resources().first()
            val edited = changed.saveResource(ResourceEditorInput(a.id, "Renamed", a.type, a.category, a.tags, a.content))
            val reopened = SessionImporter.fromJson(edited.toJson()).resources().first()
            assertEquals(status, reopened.organizationStatus())
            assertEquals(priority, reopened.resourcePriority())
            assertEquals(a.organization, reopened.organization)
        } }
    }

    @Test fun `organization never changes linked blocks logs learning content or pending practice`() {
        val original = session().setResourceLearningStatus("a", ResourceLearningStatus.PLAYABLE)
            .updateResourceLearning("a", ResourceLearningUpdate(null, 75, updateStatus = false))
            .saveRoutine(RoutineEditorInput("routine", "Routine", null, null,
                listOf(RoutineBlockInput("block", "Play", 5, 60, "a", "general", "Keep"))))
            .recordPractice(PracticeEntryInput("2026-11-12", "routine", "Routine", 1, false, emptyMap()))
            .saveActivePractice(ActivePracticeSnapshot("free", "free", "Free", "general", 0, null, mapOf("general" to 5)))
        val changed = original.updateResourceOrganization("a", ResourceOrganizationUpdate(ResourceStatus.COMPLETED, ResourcePriority.HIGH))
        assertEquals(original.toJsonObject().filterKeys { it !in setOf("resources", "user") },
            changed.toJsonObject().filterKeys { it !in setOf("resources", "user") })
        val beforeResource = ((original.toJsonObject()["resources"] as JsonArray).first() as JsonObject)
        val afterResource = ((changed.toJsonObject()["resources"] as JsonArray).first() as JsonObject)
        assertEquals(beforeResource, JsonObject(afterResource - "organization"))
        assertEquals("a", changed.routines().single().blocks.single().resourceId)
        assertEquals(75, changed.resources().first().masteryPercent())
    }

    @Test fun `unsupported organization is retained with access through all resources and partial editing`() {
        val original = session()
        val resources = original.toJsonObject()["resources"] as JsonArray
        val rawResource = resources.first() as JsonObject
        val future = Json.parseToJsonElement("""{"status":"future-status","priority":"future-priority","custom":{"keep":true}}""")
        val imported = original.withRootValue("resources", JsonArray(listOf(JsonObject(rawResource + ("organization" to future))) + resources.drop(1)))
        assertNull(imported.resources().first().organizationStatus())
        assertNull(imported.resources().first().resourcePriority())
        assertEquals(3, imported.resources().organizedResources().size)
        assertEquals(4, imported.resources().organizedResources(ResourceView.ALL).size)
        val changed = imported.updateResourceOrganization("a", ResourceOrganizationUpdate(priority = ResourcePriority.HIGH))
        val organization = changed.resources().first().organization as JsonObject
        assertEquals(JsonPrimitive("future-status"), organization["status"])
        assertEquals((future as JsonObject)["custom"], organization["custom"])
    }

    @Test fun `unchanged organization does not refresh its timestamp`() {
        val changed = session().updateResourceOrganization("a", ResourceOrganizationUpdate(ResourceStatus.HIDDEN, ResourcePriority.HIGH))
        assertSame(changed, changed.updateResourceOrganization("a", ResourceOrganizationUpdate(ResourceStatus.HIDDEN, ResourcePriority.HIGH)))
        assertSame(changed, changed.updateResourceOrganization("a", ResourceOrganizationUpdate()))
    }

    @Test fun `missing or ambiguous identities fail without changing the original`() {
        val original = session()
        val before = original.toJson()
        listOf("", "missing").forEach { id ->
            assertFailsWith<IllegalArgumentException> { original.updateResourceOrganization(id, ResourceOrganizationUpdate(ResourceStatus.HIDDEN)) }
        }
        val duplicate = original.withRootValue("resources", JsonArray((original.toJsonObject()["resources"] as JsonArray).let { it + it.first() }))
        assertFailsWith<IllegalArgumentException> { duplicate.updateResourceOrganization("a", ResourceOrganizationUpdate(ResourceStatus.HIDDEN)) }
        assertEquals(before, original.toJson())
    }
}
