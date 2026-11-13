package com.muskop.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.session.PracticeLogEntry
import com.muskop.session.SessionDocument
import com.muskop.session.activePractice
import com.muskop.session.practiceEntries
import com.muskop.session.practiceGoals
import com.muskop.session.routines
import com.muskop.session.totalMinutes
import com.muskop.session.totalWholeMinutes
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

    val playableRoutines = routines.filter { it.blocks.isNotEmpty() }
    val routine = lastRoutine?.takeIf { it.blocks.isNotEmpty() } ?: playableRoutines.firstOrNull()
    val active = remember(session) { session.activePractice() }?.takeIf { snapshot ->
        snapshot.kind == "free" ||
            (snapshot.kind == "routine" && playableRoutines.any { it.id == snapshot.routineId })
    }
    val primaryTarget = when {
        active != null -> if (active.kind == "free") FREE_PRACTICE else active.routineId
        routine != null -> routine.id
        else -> FREE_PRACTICE
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Hello, ${session.username}",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(todayLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            PracticeActionCard(
                eyebrow = when {
                    active != null -> "Continue where you left off"
                    routine != null && routine.id == lastRoutine?.id -> "Your last routine"
                    routine != null -> "Ready to practice"
                    else -> "Make time for your guitar"
                },
                title = active?.routineName ?: routine?.name ?: "Free practice",
                subtitle = when {
                    active?.kind == "routine" -> "Block ${active.blockIndex + 1} · Practice saved"
                    active != null -> "${active.elapsedBySkillSeconds.values.sum() / 60} min practiced · Practice saved"
                    routine != null -> "${routine.blocks.size} blocks · ${routine.totalMinutes()} min"
                    else -> "Play at your own pace and track your time"
                },
                icon = when {
                    active != null -> Icons.Rounded.Restore
                    routine != null -> Icons.AutoMirrored.Rounded.QueueMusic
                    else -> Icons.Rounded.Bolt
                },
                button = if (active != null) "Continue practice" else "Practice",
                onClick = { onStartPractice(primaryTarget) },
            )
        }
        if (primaryTarget != FREE_PRACTICE) {
            item {
                Card(
                    onClick = { onStartPractice(FREE_PRACTICE) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Free practice", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Play at your own pace",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Your practice",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                PracticeSummary(summary.todayMinutes, summary.weekMinutes, goals.weekly)
            }
        }
    }
}

@Composable
private fun PracticeSummary(today: Int, week: Int, weeklyTarget: Int) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (largeText) {
                SummaryMetric("Today", today)
                SummaryMetric("This week", week)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryMetric("Today", today, Modifier.weight(1f))
                    SummaryMetric("This week", week, Modifier.weight(1f))
                }
            }
            if (weeklyTarget > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weekly goal", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$week / $weeklyTarget min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { (week.toFloat() / weeklyTarget).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, minutes: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("$minutes min", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun PracticeActionCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    button: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
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
                todayMinutes = entries.filter { it.date == today }.totalWholeMinutes(),
                weekMinutes = entries.filter { it.date in weekStart..today }.totalWholeMinutes(),
            )
        }
    }
}

private fun todayLabel(): String = SimpleDateFormat("EEEE, MMMM d", Locale.US)
    .format(Calendar.getInstance().time)
    .replaceFirstChar { it.titlecase(Locale.US) }

private fun dateKey(calendar: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
