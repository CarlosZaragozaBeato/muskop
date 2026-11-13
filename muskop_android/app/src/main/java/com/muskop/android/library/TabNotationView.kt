package com.muskop.android.library

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muskop.session.tabDisplayNotes
import com.muskop.session.tabReadingRows
import com.muskop.session.TabDisplayNote
import kotlinx.serialization.json.JsonObject

@Composable
internal fun PimaReadingOptions(notes: List<TabDisplayNote>, showPima: Boolean, onShowPima: (Boolean) -> Unit) {
    val hasPima = notes.any { it.pima != null }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (hasPima) {
            TextButton(onClick = { onShowPima(!showPima) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text(if (showPima) "Hide PIMA" else "Show PIMA")
            }
            if (showPima) Text("Right hand · p thumb · i index · m middle · a ring",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val notice = when {
            notes.any { it.finger != null && it.pima == null } -> "Some finger markings are shown in the note details."
            notes.isNotEmpty() && !hasPima -> "No PIMA fingering supplied for these notes."
            showPima && notes.any { it.pima == null } -> "PIMA is not specified for some notes."
            else -> null
        }
        notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun TabMeasureLines(
    measure: JsonObject, number: Int, measureBeats: Double, stringCount: Int,
    tuning: List<String>, zoom: Float, availableWidth: Dp, showPima: Boolean,
) {
    val notes = remember(measure) { tabDisplayNotes(measure) }
    val scale = zoom * LocalDensity.current.fontScale
    val rows = remember(notes, measureBeats, stringCount, availableWidth, scale) {
        tabReadingRows(notes, measureBeats, stringCount, availableWidth.value, scale)
    }
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val textColor = MaterialTheme.colorScheme.onSurface
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val fingerColor = MaterialTheme.colorScheme.primary
    val onFingerColor = MaterialTheme.colorScheme.onPrimary
    val annotated = showPima && notes.any { it.pima != null }
    val spacingDp = if (annotated) 40f else 30f
    rows.forEachIndexed { index, row ->
        val label = "Measure $number" + if (rows.size > 1) " · Part ${index + 1}/${rows.size}" else ""
        val rowNotes = row.columns.flatMap { it.notes }
        val accessibleNotes = rowNotes.joinToString("; ") { note ->
            "beat ${note.beat}, string ${note.string}, fret ${note.fret}" +
                (note.pima?.let { ", right hand $it" } ?: ", PIMA not specified")
        }
        Column(Modifier.width(row.widthDp.dp.coerceAtMost(availableWidth))) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            // Only extreme zoom needs horizontal scroll; ordinary dense measures wrap by attack.
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                Canvas(Modifier.width(row.widthDp.dp)
                    .height(((16f + (stringCount - 1) * spacingDp + if (annotated) 32f else 18f) * scale).dp)
                    .semantics { contentDescription = "$label, $stringCount strings. $accessibleNotes" }) {
                    val unit = 1.dp.toPx() * scale
                    val top = 16f * unit
                    val spacing = spacingDp * unit
                    val bottom = top + (stringCount - 1) * spacing
                    val left = row.startXDp.dp.toPx()
                    val right = row.endXDp.dp.toPx()
                    repeat(stringCount) { string ->
                        val y = top + string * spacing
                        drawLine(lineColor, Offset(left, y), Offset(right, y), strokeWidth = 0.8f * unit)
                    }
                    repeat(measureBeats.toInt() + 1) { pulse ->
                        val beat = 1.0 + pulse
                        if (beat >= row.startBeat && beat < row.endBeat) {
                            val x = row.xAt(beat).dp.toPx()
                            drawLine(lineColor.copy(alpha = 0.25f), Offset(x, top), Offset(x, bottom), strokeWidth = 0.7f * unit)
                        }
                    }
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = Typeface.create("monospace", Typeface.BOLD)
                    }
                    tuning.asReversed().take(stringCount).forEachIndexed { string, name ->
                        paint.color = textColor.toArgb()
                        paint.textAlign = Paint.Align.RIGHT
                        paint.textSize = 12f * unit
                        drawIntoCanvas { it.nativeCanvas.drawText(name, 24f * unit, top + string * spacing + 4f * unit, paint) }
                    }
                    row.columns.forEach { column ->
                        val x = column.xDp.dp.toPx()
                        column.notes.forEach { note ->
                            val y = top + (note.string - 1) * spacing
                            paint.textSize = 16f * unit
                            paint.textAlign = Paint.Align.CENTER
                            val fret = note.fret.toString()
                            val halfWidth = maxOf(11f * unit, paint.measureText(fret) / 2f + 3f * unit)
                            drawRoundRect(surface, Offset(x - halfWidth, y - 11f * unit), Size(halfWidth * 2, 22f * unit), CornerRadius(3f * unit))
                            paint.color = textColor.toArgb()
                            drawIntoCanvas { it.nativeCanvas.drawText(fret, x, y + 5f * unit, paint) }
                            val finger = note.pima
                            if (showPima && finger != null) {
                                drawRoundRect(fingerColor, Offset(x - 8f * unit, y + 12f * unit), Size(16f * unit, 16f * unit), CornerRadius(4f * unit))
                                paint.textSize = 11f * unit
                                paint.color = onFingerColor.toArgb()
                                drawIntoCanvas { it.nativeCanvas.drawText(finger, x, y + 24f * unit, paint) }
                            }
                        }
                    }
                }
            }
        }
    }
}
