package com.muskop.android.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.session.PracticeGoals
import com.muskop.session.PracticeLogEntry
import com.muskop.session.PracticeRoutine
import com.muskop.session.SessionDocument
import com.muskop.session.practiceEntries
import com.muskop.session.practiceGoals
import com.muskop.session.routines
import com.muskop.session.skillGoals
import com.muskop.session.totalWholeMinutes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProgressScreen(
    session: SessionDocument,
    padding: PaddingValues,
    onSavePracticeGoals: (PracticeGoals) -> Unit,
) {
    val entries = remember(session) { session.practiceEntries() }
    val routines = remember(session) { session.routines() }
    val snapshot = remember(entries, routines, session) {
        ProgressSnapshot.from(entries, routines, session.practiceGoals(), session.skillGoals())
    }
    val trends = remember(entries) { PracticeTrends.from(entries) }
    val largeText = LocalDensity.current.fontScale >= 1.5f

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Progress", screenTitle = true)
                Text("Your time and practice habits", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Metric("Total practice", practiceTime(snapshot.totalMinutes), prominent = true)
                    if (largeText) {
                        Metric("This week", practiceTime(snapshot.weekMinutes))
                        Metric("Current streak", "${snapshot.streak} days")
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Metric("This week", practiceTime(snapshot.weekMinutes), Modifier.weight(1f))
                            Metric("Current streak", "${snapshot.streak} days", Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (entries.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Every practice counts")
                    Text("Finish and save a practice to start building your history.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Last 7 days")
                WeekActivity(snapshot.lastDays.asReversed())
            }
        }
        item { PracticeTrendsSection(trends) }
        item { GoalsCard(snapshot.goals, snapshot.weekMinutes, snapshot.monthMinutes, snapshot.yearMinutes, onSavePracticeGoals) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Practice by skill")
                Text("Time logged across all your practice", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entries.any { it.skillMinutes.isEmpty() }) {
                    Text("Older entries without skill details use the current routine blocks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        snapshot.focusSkill?.let { (skill, minutes) ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val hasGoalGap = (snapshot.skillGoals[skill] ?: 0) > minutes
                    Text(if (hasGoalGap) "Largest gap to a skill goal" else "Least logged of your practiced skills",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${skillLabel(skill)} · ${practiceTime(minutes)}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (snapshot.skillMinutes.isEmpty()) {
            item { Text("No practice time by skill yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(snapshot.skillMinutes.toList(), key = { it.first }) { (skill, minutes) ->
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(skillLabel(skill), style = MaterialTheme.typography.titleMedium)
                    Text(practiceTime(minutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val target = snapshot.skillGoals[skill]?.takeIf { it > 0 }
                    if (target != null) {
                        Text("Goal: $minutes / $target min", style = MaterialTheme.typography.bodySmall)
                    }
                    val denominator = target ?: snapshot.skillMinutes.values.sum().coerceAtLeast(1)
                    if (target == null) {
                        Text("${(minutes.toFloat() / denominator * 100).toInt()}% of logged skill time",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TimeProgress(minutes, denominator)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, screenTitle: Boolean = false) {
    Text(text, style = if (screenTitle) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() })
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier, prominent: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun WeekActivity(days: List<DayMinutes>) {
    val maxMinutes = days.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        if (largeText) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                days.forEach { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${dayLabel(day.date, "EEE, MMM d")} · ${day.minutes} min",
                            style = MaterialTheme.typography.bodyMedium)
                        TimeProgress(day.minutes, maxMinutes)
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { day ->
                    Column(
                        Modifier.weight(1f).clearAndSetSemantics {
                            contentDescription = "${day.date}: ${day.minutes} minutes"
                        },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("${day.minutes}", style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.fillMaxWidth().height(72.dp).clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.outlineVariant), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((day.minutes.toFloat() / maxMinutes).coerceIn(0f, 1f))
                                .background(MaterialTheme.colorScheme.primary))
                        }
                        Text(dayLabel(day.date, "EEE"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Minutes per day · ${dayLabel(days.first().date, "MMM d")} – ${dayLabel(days.last().date, "MMM d")}",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalsCard(
    goals: PracticeGoals,
    week: Int,
    month: Int,
    year: Int,
    onSavePracticeGoals: (PracticeGoals) -> Unit,
) {
    var showMore by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    if (editing) {
        PracticeGoalsEditor(goals, onDismiss = { editing = false }, onSave = {
            onSavePracticeGoals(it)
            editing = false
        })
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            SectionTitle("Practice goals")
            TextButton(onClick = { editing = true }) { Text("Edit goals") }
        }
        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GoalLine("This week", week, goals.weekly)
                if (showMore) {
                    GoalLine("This month", month, goals.monthly)
                    GoalLine("This year", year, goals.yearly)
                }
                TextButton(onClick = { showMore = !showMore },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(if (showMore) "Less detail" else "Monthly and yearly goals")
                }
            }
        }
    }
}

@Composable
private fun GoalLine(label: String, current: Int, target: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(if (target > 0) "$current / $target min${if (current >= target) " · Reached" else ""}" else "$current min · No goal set",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (target > 0) TimeProgress(current, target)
    }
}

@Composable
private fun TimeProgress(current: Int, target: Int) {
    LinearProgressIndicator(
        progress = { (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
    )
}

private fun practiceTime(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

private fun dayLabel(date: String, pattern: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return@runCatching date
    SimpleDateFormat(pattern, Locale.US).format(parsed)
}.getOrDefault(date)

private data class DayMinutes(val date: String, val minutes: Int)

private data class ProgressSnapshot(
    val totalMinutes: Int,
    val weekMinutes: Int,
    val monthMinutes: Int,
    val yearMinutes: Int,
    val streak: Int,
    val goals: PracticeGoals,
    val skillGoals: Map<String, Int>,
    val skillMinutes: Map<String, Int>,
    val focusSkill: Pair<String, Int>?,
    val lastDays: List<DayMinutes>,
) {
    companion object {
        fun from(
            entries: List<PracticeLogEntry>,
            routines: List<PracticeRoutine>,
            goals: PracticeGoals,
            skillGoals: Map<String, Int>,
        ): ProgressSnapshot {
            val totalsByDay = entries.groupBy(PracticeLogEntry::date)
                .mapValues { (_, day) -> day.totalWholeMinutes() }
            val now = Calendar.getInstance()
            val today = dateKey(now)
            val weekStart = weekStart(now)
            val monthPrefix = today.take(7)
            val yearPrefix = today.take(4)
            val skillMinutes = resolvedSkills(entries, routines)
            return ProgressSnapshot(
                totalMinutes = entries.totalWholeMinutes(),
                weekMinutes = entries.filter { it.date in weekStart..today }.totalWholeMinutes(),
                monthMinutes = entries.filter { it.date.startsWith(monthPrefix) && it.date <= today }.totalWholeMinutes(),
                yearMinutes = entries.filter { it.date.startsWith(yearPrefix) && it.date <= today }.totalWholeMinutes(),
                streak = streak(totalsByDay.keys, now),
                goals = goals,
                skillGoals = skillGoals,
                skillMinutes = skillMinutes.toList().sortedByDescending { it.second }.toMap(),
                focusSkill = focusSkill(skillMinutes, skillGoals),
                lastDays = lastDays(totalsByDay, now),
            )
        }
    }
}

private fun focusSkill(skillMinutes: Map<String, Int>, skillGoals: Map<String, Int>): Pair<String, Int>? {
    val goalFocus = skillGoals
        .filter { (skill, target) -> target > (skillMinutes[skill] ?: 0) }
        .maxByOrNull { (skill, target) -> target - (skillMinutes[skill] ?: 0) }
        ?.key
    if (goalFocus != null) return goalFocus to (skillMinutes[goalFocus] ?: 0)
    return skillMinutes
        .filterKeys { it != "general" }
        .minByOrNull { (_, minutes) -> minutes }
        ?.let { it.key to it.value }
}

private fun resolvedSkills(entries: List<PracticeLogEntry>, routines: List<PracticeRoutine>): Map<String, Int> {
    val routineSkills = routines.associate { routine ->
        routine.id to routine.blocks.groupBy { it.skill ?: "general" }.mapValues { (_, blocks) -> blocks.sumOf { it.minutes } }
    }
    val result = mutableMapOf<String, Int>()
    entries.forEach { entry ->
        val skills = entry.skillMinutes.ifEmpty { allocateLegacy(entry.minutes, routineSkills[entry.routineId]) }
        skills.forEach { (skill, minutes) -> result[skill] = (result[skill] ?: 0) + minutes }
    }
    return result
}

private fun allocateLegacy(minutes: Int, planned: Map<String, Int>?): Map<String, Int> {
    if (planned.isNullOrEmpty()) return mapOf("general" to minutes)
    val plannedTotal = planned.values.sum().coerceAtLeast(1)
    val allocated = planned.mapValues { (_, value) -> value * minutes / plannedTotal }.toMutableMap()
    var missing = minutes - allocated.values.sum()
    planned.keys.sortedByDescending { skill -> (planned.getValue(skill) * minutes) % plannedTotal }.forEach { skill ->
        if (missing > 0) {
            allocated[skill] = allocated.getValue(skill) + 1
            missing -= 1
        }
    }
    return allocated.filterValues { it > 0 }
}

private fun streak(activeDays: Set<String>, now: Calendar): Int {
    val cursor = now.clone() as Calendar
    if (dateKey(cursor) !in activeDays) cursor.add(Calendar.DAY_OF_YEAR, -1)
    var count = 0
    while (dateKey(cursor) in activeDays) {
        count += 1
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }
    return count
}

private fun lastDays(byDay: Map<String, Int>, now: Calendar): List<DayMinutes> {
    val cursor = now.clone() as Calendar
    return List(7) {
        val date = dateKey(cursor)
        cursor.add(Calendar.DAY_OF_YEAR, -1)
        DayMinutes(date, byDay[date] ?: 0)
    }
}

private fun weekStart(calendar: Calendar): String {
    val start = calendar.clone() as Calendar
    val offset = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7
    start.add(Calendar.DAY_OF_YEAR, -offset)
    return dateKey(start)
}

private fun dateKey(calendar: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

private fun skillLabel(skill: String): String = when (skill) {
    "arpegios" -> "Arpeggios"
    "cambios-acordes" -> "Chord changes"
    "patrones" -> "Patterns"
    "velocidad" -> "Speed"
    "tecnica" -> "Technique"
    "repertorio" -> "Repertoire"
    else -> "General practice"
}
