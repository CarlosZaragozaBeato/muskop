package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muskop.session.ConceptLocation
import com.muskop.session.ResourceConnection
import com.muskop.session.ResourceConnectionKind
import com.muskop.session.ResourceMusicalMetadata
import com.muskop.session.SessionResource
import com.muskop.session.musicalMetadata

internal fun LazyListScope.resourceConnectionItems(
    metadata: ResourceMusicalMetadata?,
    connections: List<ResourceConnection>,
    onOpen: (SessionResource) -> Unit,
) {
    item(key = "musical-profile") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your practice connections", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            if (metadata == null) {
                Text("This resource has no musical study information yet. Import a resource that includes the chords, patterns or techniques it trains or uses.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                metadata.role?.let { role -> Text(when (role) {
                    "exercise" -> "Exercise"
                    "song" -> "Song"
                    "fragment" -> "Fragment"
                    else -> "Theory"
                }, style = MaterialTheme.typography.labelLarge) }
                val difficulty = metadata.difficulty
                Text(if (difficulty == null) "Difficulty not assessed" else "Difficulty ${difficulty.level} of 5",
                    style = MaterialTheme.typography.titleMedium)
                difficulty?.reasons?.forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (metadata.concepts.isEmpty()) {
                    Text("No musical concepts are listed for this resource yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    metadata.concepts.groupBy { it.relation }.forEach { (relation, concepts) ->
                        Text("${when (relation) { "trains" -> "Trains"; "uses" -> "Uses"; else -> "Explains" }}: ${concepts.joinToString { it.label }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Connections show shared study material. They do not measure your mastery of a piece.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    val study = metadata?.takeIf { it.concepts.isNotEmpty() } ?: return
    ResourceConnectionKind.entries.forEach { kind ->
        val group = connections.filter { it.kind == kind }
        val relevant = when (kind) {
            ResourceConnectionKind.PREPARATION -> study.concepts.any { it.relation == "uses" }
            ResourceConnectionKind.APPLICATION -> study.concepts.any { it.relation in setOf("trains", "explains") }
            ResourceConnectionKind.EXPLANATION -> study.concepts.any { it.relation in setOf("uses", "trains") }
        }
        if (!relevant) return@forEach
        item(key = "connections-${kind.name}") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(when (kind) {
                    ResourceConnectionKind.PREPARATION -> if (study.role == "song") "Prepare this piece" else "Prepare this resource"
                    ResourceConnectionKind.APPLICATION -> "Useful for"
                    ResourceConnectionKind.EXPLANATION -> "Understand the concepts"
                }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(when (kind) {
                    ResourceConnectionKind.PREPARATION -> "Resources that train concepts used here."
                    ResourceConnectionKind.APPLICATION -> "Resources that apply the concepts you study here."
                    ResourceConnectionKind.EXPLANATION -> "Explanations of concepts you use or train here."
                }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (group.isEmpty()) Text("No matching resources in your library yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(group, key = { "${kind.name}-${it.resource.id}" }) { connection ->
            ResourceConnectionCard(connection, onOpen)
        }
    }
}

@Composable
internal fun ResourceConnectionCard(connection: ResourceConnection, onOpen: (SessionResource) -> Unit) {
    Card(onClick = { onOpen(connection.resource) }, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(connection.resource.title, style = MaterialTheme.typography.titleMedium)
                    connection.resource.learningSummaryLabel()?.let {
                        Text("Learning · $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    connection.resource.musicalMetadata()?.difficulty?.let {
                        Text("Difficulty ${it.level} of 5", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
            connection.concepts.forEach { concept ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(concept.label, style = MaterialTheme.typography.labelLarge)
                    Text("Here: ${locationLabel(concept.sourceLocation)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("In ${connection.resource.title}: ${locationLabel(concept.targetLocation)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

internal fun locationLabel(location: ConceptLocation): String = buildList {
    if (location.wholeResource) add("Whole resource")
    addAll(location.sectionNames)
    if (location.unavailableSectionIds.isNotEmpty()) add("A referenced section is no longer available")
}.joinToString(" · ")
