package com.muskop.android.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.muskop.session.ResourceView
import com.muskop.session.organizedResources
import com.muskop.android.library.ResourceViewPicker
import com.muskop.android.library.organizationLabel
import com.muskop.session.SessionResource

@Composable
internal fun PracticeResourceChooser(
    resources: List<SessionResource>,
    selected: SessionResource?,
    onSelect: (SessionResource?) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var view by rememberSaveable { mutableStateOf(ResourceView.ACTIVE) }
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    val types = remember(resources) { resources.map { it.type }.distinct().sorted() }
    val results = remember(resources, query, type, view) {
        val term = query.trim()
        resources.organizedResources(view).filter { resource ->
            (type == null || resource.type == type) &&
                (term.isEmpty() || (listOf(resource.title, resource.category.orEmpty(), resource.type, practiceResourceTypeLabel(resource.type)) + resource.tags)
                    .any { it.contains(term, ignoreCase = true) })
        }
    }

    if (selected == null) {
        Card(
            onClick = { view = ResourceView.ACTIVE; open = true },
            enabled = resources.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (resources.isEmpty()) "Practice without a resource" else "Add a practice resource",
                        style = MaterialTheme.typography.titleMedium)
                    Text(if (resources.isEmpty()) "Play from memory. You can add resources in Library later."
                        else "Optional · Search your library", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (resources.isNotEmpty()) Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Practice resource", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected.title, style = MaterialTheme.typography.titleMedium)
                selected.organizationLabel()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            IconButton(onClick = { view = ResourceView.ACTIVE; open = true }) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Change practice resource")
            }
            IconButton(onClick = { onSelect(null) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove practice resource")
            }
        }
    }
    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Choose a resource", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { open = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close resource picker")
                        }
                    }
                    ResourceViewPicker(view) { view = it }
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Search resources") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        },
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = type == null, onClick = { type = null }, label = { Text("All types") }) }
                        items(types, key = { it }) { value ->
                            FilterChip(selected = type == value, onClick = { type = if (type == value) null else value },
                                label = { Text(practiceResourceTypeLabel(value)) })
                        }
                    }
                    Text("${results.size} ${if (results.size == 1) "resource" else "resources"}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (results.isEmpty()) item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No matching resources. Try another search or choose All resources.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { query = ""; type = null; view = ResourceView.ALL },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                                    Text("Show all resources")
                                }
                            }
                        }
                        items(results, key = { it.id }) { resource ->
                            Card(
                                onClick = { onSelect(resource); open = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (resource.id == selected?.id)
                                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                    contentColor = if (resource.id == selected?.id) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(resource.title, style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(listOfNotNull(practiceResourceTypeLabel(resource.type), resource.category?.takeIf(String::isNotBlank)).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall)
                                    resource.organizationLabel()?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                                    if (resource.tags.isNotEmpty()) Text(resource.tags.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun practiceResourceTypeLabel(type: String): String = when (type.uppercase(java.util.Locale.ROOT)) {
    "TAB" -> "Tablature"
    "THEORY" -> "Theory"
    "CHORD" -> "Chords"
    "SNIPPET" -> "Snippet"
    "COLLECTION" -> "Collection"
    "MEDIA" -> "Media"
    else -> type
}
