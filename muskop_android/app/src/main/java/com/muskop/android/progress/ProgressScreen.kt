package com.muskop.android.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.session.PracticeGoals
import com.muskop.session.PracticeLogEntry
import com.muskop.session.PracticeRoutine
import com.muskop.session.SessionDocument
import com.muskop.session.practiceEntries
import com.muskop.session.practiceGoals
import com.muskop.session.routines
import com.muskop.session.skillGoals
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProgressScreen(session: SessionDocument, padding: PaddingValues) {
    val entries = remember(session) { session.practiceEntries() }
    val routines = remember(session) { session.routines() }
    val snapshot = remember(entries, routines, session) {
        ProgressSnapshot.from(entries, routines, session.practiceGoals(), session.skillGoals())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Progreso", style = MaterialTheme.typography.headlineMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Total", "${snapshot.totalMinutes} min", Modifier.weight(1f))
                MetricCard("Semana", "${snapshot.weekMinutes} min", Modifier.weight(1f))
                MetricCard("Racha", "${snapshot.streak} días", Modifier.weight(1f))
            }
        }
        item { GoalsCard(snapshot.goals, snapshot.weekMinutes, snapshot.monthMinutes, snapshot.yearMinutes) }
        snapshot.focusSkill?.let { (skill, minutes) ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Siguiente foco", style = MaterialTheme.typography.titleLarge)
                        Text("Refuerza ${skillLabel(skill)}: llevas $minutes min registrados.")
                    }
                }
            }
        }
        item { Text("Por habilidad", style = MaterialTheme.typography.titleLarge) }
        if (snapshot.skillMinutes.isEmpty()) {
            item { Text("Aún no hay minutos de práctica para analizar.") }
        }
        items(snapshot.skillMinutes.toList(), key = { it.first }) { (skill, minutes) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(skillLabel(skill), fontWeight = FontWeight.SemiBold)
                        snapshot.skillGoals[skill]?.let { goal -> Text("Objetivo: $minutes / $goal min") }
                    }
                    Text("$minutes min")
                }
            }
        }
        item { Text("Últimos días", style = MaterialTheme.typography.titleLarge) }
        items(snapshot.lastDays, key = { it.date }) { day ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(day.date)
                Text("${day.minutes} min")
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun GoalsCard(goals: PracticeGoals, week: Int, month: Int, year: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Objetivos", style = MaterialTheme.typography.titleLarge)
            GoalLine("Semana", week, goals.weekly)
            GoalLine("Mes", month, goals.monthly)
            GoalLine("Año", year, goals.yearly)
        }
    }
}

@Composable
private fun GoalLine(label: String, current: Int, target: Int) {
    val text = if (target > 0) "$current / $target min" else "$current min · sin objetivo"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(text)
    }
}

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
            val totalsByDay = entries.groupBy(PracticeLogEntry::date).mapValues { (_, day) -> day.sumOf(PracticeLogEntry::minutes) }
            val now = Calendar.getInstance()
            val today = dateKey(now)
            val weekStart = weekStart(now)
            val monthPrefix = today.take(7)
            val yearPrefix = today.take(4)
            val skillMinutes = resolvedSkills(entries, routines)
            return ProgressSnapshot(
                totalMinutes = entries.sumOf(PracticeLogEntry::minutes),
                weekMinutes = entries.filter { it.date in weekStart..today }.sumOf(PracticeLogEntry::minutes),
                monthMinutes = entries.filter { it.date.startsWith(monthPrefix) && it.date <= today }.sumOf(PracticeLogEntry::minutes),
                yearMinutes = entries.filter { it.date.startsWith(yearPrefix) && it.date <= today }.sumOf(PracticeLogEntry::minutes),
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
    "arpegios" -> "Arpegios"
    "cambios-acordes" -> "Cambios de acordes"
    "patrones" -> "Patrones"
    "velocidad" -> "Velocidad"
    "tecnica" -> "Técnica"
    "repertorio" -> "Repertorio"
    else -> "Práctica general"
}
