package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json

class TabImportTest {
    @Test fun `import retains the complete document`() {
        val raw = """{"version":2,"title":"Study","custom":{"future":true},"sections":[{"kind":"tab","measures":[{"events":[{"beat":1,"duration":"eighth","notes":[{"string":1,"fret":3,"technique":"slide","extra":"keep"}]}]}]}]}"""
        assertEquals(Json.parseToJsonElement(raw), importTabDocument(raw))
    }
    @Test fun `rejects invalid or unrecognised documents`() {
        assertFailsWith<IllegalArgumentException> { importTabDocument("{") }
        assertFailsWith<IllegalArgumentException> { importTabDocument("""{"version":3,"title":"Study","sections":[]}""") }
        assertFailsWith<IllegalArgumentException> { importTabDocument("""{"version":2,"title":"Study","sections":[1]}""") }
    }
    @Test fun `reports the location of invalid notes without restricting musical values`() {
        val bad = """{"version":2,"title":"Study","sections":[{"measures":[{"events":[{"beat":1,"notes":[{"string":"first","fret":0}]}]}]}]}"""
        val error = assertFailsWith<IllegalArgumentException> { importTabDocument(bad) }
        kotlin.test.assertTrue(error.message.orEmpty().contains("Section 1, measure 1, event 1, note 1"))
        val flexible = """{"version":2,"title":"Study","timeSignature":"7/8","sections":[{"measures":[{"events":[{"beat":1.333,"notes":[{"string":7,"fret":27,"technique":"custom"}]}]}]}]}"""
        assertEquals(Json.parseToJsonElement(flexible), importTabDocument(flexible))
    }
}
