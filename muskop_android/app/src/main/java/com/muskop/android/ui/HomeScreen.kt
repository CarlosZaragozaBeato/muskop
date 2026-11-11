package com.muskop.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.session.PracticeLogEntry
import com.muskop.session.SessionDocument
import com.muskop.session.practiceEntries
import com.muskop.session.practiceGoals
import com.muskop.session.routines
import com.muskop.session.totalMinutes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val FREE_PRACTICE = "free"

@Composable
fun HomeScreen(session: SessionDocument, padding: PaddingValues, onStartPractice: (String) -> Unit) {
    val entries = remember(session) { session.practiceEntries() }
    val routines = remember(session) { session.routines() }
    val goals = remember(session) { session.practiceGoals() }
    val summary = remember(entries) { HomeSummary.from(entries) }
    val lastRoutine = remember(entries, routines) {
        entries.asReversed().firstNotNullOfOrNull { entry ->
            routines.firstOrNull { it.id == entry.routineId }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Hello, ${session.username}", style = MaterialTheme.typography.headlineMedium)
                Text(todayLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Today", summary.todayMinutes, Icons.Rounded.Timer, Modifier.weight(1f))
                SummaryCard("This week", summary.weekMinutes, Icons.Rounded.CalendarToday, Modifier.weight(1f))
            }
        }
        if (goals.weekly > 0) {
            item { WeeklyGoal(summary.weekMinutes, goals.weekly) }
        }
        item { Text("Start practicing", style = MaterialTheme.typography.titleLarge) }
        item {
            PracticeActionCard(
                title = "Free practice",
                subtitle = "Play at your own pace and track your time",
                icon = Icons.Rounded.Bolt,
                button = "Start",
                onClick = { onStartPractice(FREE_PRACTICE) },
            )
        }
        lastRoutine?.let { routine ->
            item {
                PracticeActionCard(
                    title = routine.name,
                    subtitle = "Last routine · ${routine.totalMinutes()} min",
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    button = "Repeat",
                    onClick = { onStartPractice(routine.id) },
                )
            }
        }
        if (lastRoutine == null && routines.isNotEmpty()) {
            val firstRoutine = routines.first()
            item {
                PracticeActionCard(
                    title = firstRoutine.name,
                    subtitle = "Suggested routine · ${firstRoutine.totalMinutes()} min",
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    button = "Practice",
                    onClick = { onStartPractice(firstRoutine.id) },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, minutes: Int, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("$minutes min", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WeeklyGoal(current: Int, target: Int) {
    val progress = (current.toFloat() / target).coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Weekly goal", fontWeight = FontWeight.SemiBold)
                Text("$current / $target min", color = MaterialTheme.colorScheme.primary)
            }
            Box(
                Modifier.fillMaxWidth().height(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier.fillMaxWidth(progress).height(8.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun PracticeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    button: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(button)
            }
        }
    }
}

private data class HomeSummary(val todayMinutes: Int, val weekMinutes: Int) {
    companion object {
        fun from(entries: List<PracticeLogEntry>): HomeSummary {
            val now = Calendar.getInstance()
            val today = dateKey(now)
            val start = now.clone() as Calendar
            start.add(Calendar.DAY_OF_YEAR, -((start.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            val weekStart = dateKey(start)
            return HomeSummary(
                todayMinutes = entries.filter { it.date == today }.sumOf(PracticeLogEntry::minutes),
                weekMinutes = entries.filter { it.date in weekStart..today }.sumOf(PracticeLogEntry::minutes),
            )
        }
    }
}

private fun todayLabel(): String = SimpleDateFormat("EEEE, MMMM d", Locale.US)
    .format(Calendar.getInstance().time)
    .replaceFirstChar { it.titlecase(Locale.US) }

private fun dateKey(calendar: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
