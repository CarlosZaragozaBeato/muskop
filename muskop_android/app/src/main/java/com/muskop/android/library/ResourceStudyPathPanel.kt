package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.session.ResourceStudyPath
import com.muskop.session.SessionResource

internal fun LazyListScope.resourceStudyPathItems(
    path: ResourceStudyPath,
    onOpen: (SessionResource) -> Unit,
    onReadTarget: () -> Unit,
) {
    item(key = "path-introduction") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Study path", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Text("Target: ${path.target.title}", style = MaterialTheme.typography.titleMedium)
            if (path.requirements.isEmpty()) {
                Text("This resource does not list the concepts it applies. A study path needs that information to find preparation in your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onReadTarget) { Text("Read resource") }
            } else {
                Text("Explore the explanations, practice a matching exercise, then apply it to your target. Choose what helps with your current difficulty.")
                Text("${path.requirements.size - path.withoutPreparation.size} of ${path.requirements.size} listed concepts have preparation in your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("This describes available material, not readiness or mastery. The path updates as your library changes.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (path.requirements.isEmpty()) return

    item(key = "path-understand") {
        StudyPathHeading("1 · Understand", "Optional explanations of concepts used by the target.")
        if (path.explanations.isEmpty()) Text("No matching explanations in your library yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(path.explanations, key = { "path-explain-${it.resource.id}" }) { ResourceConnectionCard(it, onOpen) }
    item(key = "path-prepare") {
        StudyPathHeading("2 · Prepare", "These resources train concepts the target uses. Pick an exercise for the concept you want to work on; you do not need to do every alternative.")
        if (path.preparation.isEmpty()) Text("No matching preparation resources in your library yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(path.preparation, key = { "path-prepare-${it.resource.id}" }) { ResourceConnectionCard(it, onOpen) }
    if (path.withoutPreparation.isNotEmpty()) {
        item(key = "path-missing") {
            StudyPathHeading("Preparation to add", "These listed concepts have no training resource in your library. You can ask your content agent for a focused exercise.")
        }
        items(path.withoutPreparation, key = { "path-gap-${it.id}" }) { concept ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(concept.label, style = MaterialTheme.typography.titleSmall)
                Text("In the target: ${locationLabel(concept.targetLocation)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    item(key = "path-apply") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyPathHeading("3 · Apply", path.target.title)
            path.target.learningSummaryLabel()?.let {
                Text("Your assessment · $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Try the target at a comfortable tempo. Use its study notes to choose a section and what to listen for.")
            path.requirements.forEach { concept ->
                Text("${concept.label} · ${locationLabel(concept.targetLocation)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onReadTarget) { Text("Read target") }
            Text("Use Learning status above to record your own assessment after practicing.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StudyPathHeading(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
