package com.muskop.session

import java.util.Locale
import kotlin.math.floor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** Read projection only; the document remains untouched, including unsupported finger values. */
data class TabDisplayNote(val beat: Double, val string: Int, val fret: Int, val finger: String?) {
    val pima: String? get() = finger?.trim()?.lowercase(Locale.ROOT)?.takeIf { it in setOf("p", "i", "m", "a") }
}

fun tabDisplayNotes(measure: JsonObject): List<TabDisplayNote> = (measure["events"] as? JsonArray)
    .orEmpty().flatMap { item ->
        val event = item as? JsonObject ?: return@flatMap emptyList()
        val beat = (event["beat"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }
            ?: return@flatMap emptyList()
        (event["notes"] as? JsonArray).orEmpty().mapNotNull { noteItem ->
            val note = noteItem as? JsonObject ?: return@mapNotNull null
            val string = (note["string"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            val fret = (note["fret"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            TabDisplayNote(beat, string, fret, (note["finger"] as? JsonPrimitive)?.contentOrNull)
        }
    }

data class TabReadingColumn(val beat: Double, val xDp: Float, val notes: List<TabDisplayNote>)

data class TabReadingRow(
    val widthDp: Float,
    val startBeat: Double,
    val endBeat: Double,
    val startXDp: Float,
    val endXDp: Float,
    val columns: List<TabReadingColumn>,
) {
    /** Interpolate pulse guides through the same spacing as notes, including expanded dense attacks. */
    fun xAt(beat: Double): Float {
        val anchors = (listOf(startBeat to startXDp) + columns.map { it.beat to it.xDp } + (endBeat to endXDp))
            .distinctBy { it.first }
        val bounded = beat.coerceIn(startBeat, endBeat)
        val pair = anchors.zipWithNext().firstOrNull { (a, b) -> bounded in a.first..b.first }
            ?: return endXDp
        val (a, b) = pair
        return a.second + ((bounded - a.first) / (b.first - a.first)).toFloat() * (b.second - a.second)
    }
}

/** Paginate complete attacks, keeping simultaneous notes together and a minimum horizontal gap. */
fun tabReadingRows(
    notes: List<TabDisplayNote>, measureBeats: Double, stringCount: Int,
    availableWidthDp: Float, scale: Float,
): List<TabReadingRow> {
    require(measureBeats.isFinite() && measureBeats > 0 && scale.isFinite() && scale > 0)
    val groups = notes.filter { it.string in 1..stringCount && it.beat >= 1 && it.beat < 1 + measureBeats }
        .groupBy { it.beat }.toSortedMap().entries.toList()
    val fretWidth = (groups.flatMap { it.value }.maxOfOrNull { it.fret.toString().length } ?: 2) * 10f + 6f
    val left = maxOf(40f, fretWidth / 2f + 24f) * scale
    val rightPadding = maxOf(20f, fretWidth / 2f + 4f) * scale
    val gap = maxOf(40f, fretWidth + 8f) * scale
    // Extreme zoom/font size may require scrolling a single row; never shrink annotations to fit.
    val available = availableWidthDp.coerceAtLeast(left + rightPadding + 44f * scale)
    val capacity = (floor((available - left - rightPadding) / gap).toInt() + 1).coerceAtLeast(1)
    val chunks = groups.chunked(capacity).ifEmpty { listOf(emptyList()) }
    return chunks.mapIndexed { index, chunk ->
        val start = if (index == 0) 1.0 else chunk.first().key
        val end = chunks.getOrNull(index + 1)?.first()?.key ?: (1.0 + measureBeats)
        val width = maxOf(240f * scale, left + rightPadding + (chunk.size - 1).coerceAtLeast(0) * gap).coerceAtMost(available)
        val right = width - rightPadding
        val extra = (right - left - (chunk.size - 1).coerceAtLeast(0) * gap).coerceAtLeast(0f)
        val columns = chunk.mapIndexed { columnIndex, entry ->
            val x = left + columnIndex * gap + ((entry.key - start) / (end - start)).toFloat() * extra
            TabReadingColumn(entry.key, x, entry.value)
        }
        TabReadingRow(width, start, end, left, right, columns)
    }
}
