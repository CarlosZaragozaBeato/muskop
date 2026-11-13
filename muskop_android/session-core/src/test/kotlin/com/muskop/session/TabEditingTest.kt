package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TabEditingTest {
    @Test
    fun `editing sections without ids preserves their musical content`() {
        val original = Json.parseToJsonElement("""{"custom":true,"sections":[{"kind":"tab","name":"A","measures":[{"events":[{"beat":1,"notes":[{"string":1,"fret":3}]}]}],"extra":"keep"}]}""")
        val prepared = ensureTabSectionIds(original)
        val section = prepared.jsonObject.getValue("sections").jsonArray.single().jsonObject
        val id = section.getValue("id").jsonPrimitive.content
        val saved = updateTabSections(prepared, listOf(TabSectionInput(id, "tab", "Edited", null, null)))
        val result = saved.jsonObject.getValue("sections").jsonArray.single().jsonObject
        assertEquals(section["measures"], result["measures"])
        assertEquals(section["extra"], result["extra"])
        assertEquals(original.jsonObject["custom"], saved.jsonObject["custom"])
    }

    @Test
    fun `editing notes preserves simultaneous events and unknown fields`() {
        val measure = Json.parseToJsonElement("""{"events":[{"beat":1,"duration":"eighth","extra":true,"notes":[{"string":1,"fret":3,"technique":"slide"},{"string":2,"fret":1}]},{"beat":1,"custom":"keep","notes":[{"string":3,"fret":2}]}]}""").jsonObject
        val updated = updateMeasureNote(measure, 1.0, 1, 5, null)
        val events = updated.getValue("events").jsonArray
        assertEquals(measure.getValue("events").jsonArray[1], events[1])
        val event = events[0].jsonObject
        assertEquals("eighth", event.getValue("duration").jsonPrimitive.content)
        assertNotNull(event["extra"])
        assertEquals("slide", event.getValue("notes").jsonArray.last().jsonObject.getValue("technique").jsonPrimitive.content)
        assertEquals(2, event.getValue("notes").jsonArray.size)
        val removed = updateMeasureNote(updated, 1.0, 3, null, null)
        assertEquals("keep", removed.getValue("events").jsonArray[1].jsonObject.getValue("custom").jsonPrimitive.content)
    }
}
