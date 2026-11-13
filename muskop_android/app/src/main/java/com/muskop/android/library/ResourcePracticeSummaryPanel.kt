package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.session.ResourcePracticeSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ResourcePracticeSummaryPanel(summary: ResourcePracticeSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Recorded practice", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        if (summary.totalSeconds == 0L) {
            Text("No practice time recorded for this resource yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Older practice without resource details is not included.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val seconds = summary.totalSeconds
            val duration = buildList {
                if (seconds >= 3600) add("${seconds / 3600}h")
                if (seconds >= 60) add("${seconds % 3600 / 60}m")
                if (seconds % 60 > 0) add("${seconds % 60}s")
            }.joinToString(" ")
            Text("$duration · ${summary.practiceCount} ${if (summary.practiceCount == 1) "practice" else "practices"}")
            val date = summary.lastPracticedAt?.let { at ->
                DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault()).format(Instant.parse(at))
            }
            Text(if (date == null) "Last practice time unavailable" else "Last practiced $date",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Saved practices only", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
