package com.muskop.session

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class TabReadingLayoutTest {
    @Test fun `projection preserves simultaneous notes fingering and original unknown content`() {
        val measure = Json.parseToJsonElement("""{"future":true,"events":[{"beat":1,"duration":"quarter","notes":[
          {"string":6,"fret":0,"finger":"p","technique":"slide","custom":1},
          {"string":3,"fret":2,"finger":"i"},{"string":2,"fret":1,"finger":"m"},{"string":1,"fret":0,"finger":"a"}]}]}""") as JsonObject
        val before = measure.toString()
        val notes = tabDisplayNotes(measure)
        assertEquals(listOf("p", "i", "m", "a"), notes.map { it.pima })
        val rows = tabReadingRows(notes, 4.0, 6, 280f, 1f)
        assertEquals(1, rows.single().columns.size)
        assertEquals(notes, rows.single().columns.single().notes)
        assertEquals(before, measure.toString())
    }

    @Test fun `missing unsupported and uppercase fingers never get inferred from the string`() {
        val notes = tabDisplayNotes(Json.parseToJsonElement("""{"events":[{"beat":1,"notes":[
          {"string":6,"fret":0},{"string":3,"fret":2,"finger":"unknown"},
          {"string":2,"fret":1,"finger":"P"},{"string":1,"fret":0,"finger":"1"}]}]}""") as JsonObject)
        assertNull(notes[0].pima)
        assertNull(notes[1].pima)
        assertEquals("unknown", notes[1].finger)
        assertEquals("p", notes[2].pima)
        assertEquals("P", notes[2].finger)
        assertNull(notes[3].pima)
    }

    @Test fun `very close attacks wrap without omissions or collisions across widths and zoom`() {
        val notes = (0 until 30).flatMap { index ->
            listOf(TabDisplayNote(1.0 + index * 0.001, 6, 12, "p"), TabDisplayNote(1.0 + index * 0.001, 1, 10, "a"))
        }
        listOf(240f, 280f, 360f, 640f).forEach { width ->
            listOf(0.5f, 1f, 2f, 6f).forEach { scale ->
                val rows = tabReadingRows(notes, 4.0, 6, width, scale)
                assertEquals(notes, rows.flatMap { it.columns }.flatMap { it.notes })
                rows.forEach { row ->
                    row.columns.zipWithNext().forEach { (a, b) -> assertTrue(b.xDp - a.xDp >= 40f * scale - 0.001f) }
                    row.columns.forEach { column ->
                        assertTrue(column.xDp >= row.startXDp)
                        assertTrue(column.xDp <= row.endXDp + 0.001f)
                        assertTrue(abs(column.xDp - row.xAt(column.beat)) < 0.001f)
                    }
                }
            }
        }
    }

    @Test fun `continuation boundaries retain every attack exactly once and preserve source beats`() {
        val notes = (0 until 16).map { TabDisplayNote(1.0 + it * 0.25, 1, it, "a") }
        val rows = tabReadingRows(notes, 4.0, 6, 240f, 1f)
        assertTrue(rows.size > 1)
        assertEquals(notes, rows.flatMap { it.columns }.flatMap { it.notes })
        rows.zipWithNext().forEach { (a, b) -> assertEquals(a.endBeat, b.startBeat) }
        rows.forEach { row -> row.columns.forEach { assertTrue(it.beat >= row.startBeat && it.beat < row.endBeat) } }
        assertEquals(1.0, rows.first().startBeat)
        assertEquals(5.0, rows.last().endBeat)
    }

    @Test fun `empty measures and supported signatures produce bounded readable rows`() {
        val empty = tabReadingRows(emptyList(), 4.0, 6, 280f, 1f).single()
        assertTrue(empty.columns.isEmpty())
        assertEquals(empty.startXDp, empty.xAt(1.0))
        assertEquals(empty.endXDp, empty.xAt(5.0))
        val notes = listOf(TabDisplayNote(1.0, 7, 27, "p"), TabDisplayNote(4.25, 1, 12, "a"), TabDisplayNote(4.5, 1, 2, null))
        val rows = tabReadingRows(notes, 3.5, 7, 280f, 1f)
        assertEquals(notes.take(2), rows.flatMap { it.columns }.flatMap { it.notes })
    }

    @Test fun `unsupported positions stay in the projection for details and invalid fields do not crash it`() {
        val raw = """{"events":[null,{"beat":{},"notes":[]},{"beat":1,"notes":[null,{"string":{},"fret":0},
          {"string":25,"fret":2,"finger":"p"},{"string":1,"fret":3}]},{"beat":0,"notes":[{"string":1,"fret":0}]}]}"""
        val notes = tabDisplayNotes(Json.parseToJsonElement(raw) as JsonObject)
        assertEquals(3, notes.size)
        assertEquals(1, tabReadingRows(notes, 4.0, 6, 280f, 1f).flatMap { it.columns }.flatMap { it.notes }.size)
    }

    @Test fun `extreme imported frets receive enough space without rewriting them`() {
        val notes = listOf(TabDisplayNote(1.0, 1, Int.MAX_VALUE, "a"), TabDisplayNote(1.01, 1, 0, "a"))
        val rows = tabReadingRows(notes, 4.0, 6, 240f, 1f)
        assertEquals(notes, rows.flatMap { it.columns }.flatMap { it.notes })
        rows.forEach { row -> row.columns.forEach { assertTrue(it.xDp >= 56f && row.widthDp - it.xDp >= 56f) } }
    }
}
