package com.muskop.android.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.muskop.session.PracticeLogEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal data class TrendPoint(val label: String, val minutes: Int)

internal data class PracticeTrends(
    val monthMinutes: Int,
    val priorMonthMinutes: Int,
    val yearMinutes: Int,
    val priorYearMinutes: Int,
    val activeDaysThisMonth: Int,
    val monthWeeks: List<TrendPoint>,
    val yearMonths: List<TrendPoint>,
) {
    companion object {
        fun from(entries: List<PracticeLogEntry>, now: Calendar = Calendar.getInstance()): PracticeTrends {
            val today = key(now)
            val monthStart = (now.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            val priorMonth = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val yearStart = (now.clone() as Calendar).apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val priorYear = (yearStart.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
            val currentMonth = key(monthStart).take(7)
            val previousMonth = key(priorMonth).take(7)
            val currentYear = key(yearStart).take(4)
            val previousYear = key(priorYear).take(4)
            val current = entries.filter { it.date <= today }
            fun minutes(items: List<PracticeLogEntry>) = items.sumOf(PracticeLogEntry::seconds) / 60
            val thisMonth = current.filter { it.date.startsWith(currentMonth) }
            val weeks = (1..now.getActualMaximum(Calendar.DAY_OF_MONTH)).chunked(7).map { days ->
                val start = days.first()
                val end = days.last()
                val range = days.map { day -> "$currentMonth-${day.toString().padStart(2, '0')}" }.toSet()
                TrendPoint("$start–$end", minutes(thisMonth.filter { it.date in range }))
            }
            val months = (0..11).map { index ->
                val date = (yearStart.clone() as Calendar).apply { set(Calendar.MONTH, index) }
                val prefix = key(date).take(7)
                TrendPoint(
                    SimpleDateFormat("MMM", Locale.US).format(date.time),
                    minutes(current.filter { it.date.startsWith(prefix) }),
                )
            }
            return PracticeTrends(
                monthMinutes = minutes(thisMonth),
                priorMonthMinutes = minutes(current.filter { it.date.startsWith(previousMonth) }),
                yearMinutes = minutes(current.filter { it.date.startsWith(currentYear) }),
                priorYearMinutes = minutes(current.filter { it.date.startsWith(previousYear) }),
                activeDaysThisMonth = thisMonth.filter { it.seconds > 0 }.map { it.date }.distinct().size,
                monthWeeks = weeks,
                yearMonths = months,
            )
        }

        private fun key(calendar: Calendar) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}

@Composable
internal fun PracticeTrendsSection(trends: PracticeTrends) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TrendCard(
            title = "This month",
            total = trends.monthMinutes,
            comparison = "Last month (full): ${timeLabel(trends.priorMonthMinutes)}",
            detail = "${trends.activeDaysThisMonth} active days",
            axis = "Week of month · minutes practiced",
            points = trends.monthWeeks,
        )
        TrendCard(
            title = "This year",
            total = trends.yearMinutes,
            comparison = "Last year (full): ${timeLabel(trends.priorYearMinutes)}",
            detail = null,
            axis = "Month · minutes practiced",
            points = trends.yearMonths,
        )
    }
}

@Composable
private fun TrendCard(
    title: String,
    total: Int,
    comparison: String,
    detail: String?,
    axis: String,
    points: List<TrendPoint>,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val maximum = points.maxOfOrNull(TrendPoint::minutes)?.coerceAtLeast(1) ?: 1
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(timeLabel(total), style = MaterialTheme.typography.headlineSmall)
            Text(comparison + (detail?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (total == 0) {
                Text("No practice logged in this period yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (largeText) {
                points.forEach { point ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${point.label} · ${timeLabel(point.minutes)}", style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { point.minutes.toFloat() / maximum },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    points.forEach { point ->
                        Column(
                            Modifier.weight(1f).clearAndSetSemantics {
                                contentDescription = "${point.label}: ${point.minutes} minutes practiced"
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(if (point.minutes > 0) compactMinutes(point.minutes) else "–",
                                style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Box(Modifier.fillMaxWidth().height(76.dp).clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.BottomCenter) {
                                Box(Modifier.fillMaxWidth()
                                    .fillMaxHeight(point.minutes.toFloat() / maximum)
                                    .background(MaterialTheme.colorScheme.primary))
                            }
                            Text(if (points.size > 6) point.label.take(2) else point.label,
                                style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
            Text(axis, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun compactMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h+"
}

private fun timeLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
