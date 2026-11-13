package com.muskop.android.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muskop.session.SessionDocument
import com.muskop.session.SessionResource
import com.muskop.session.ResourceImportBatch
import com.muskop.session.ResourceEditorInput
import com.muskop.session.TabSection
import com.muskop.session.resources
import com.muskop.session.tabSections
import com.muskop.session.tabMeasureLength
import com.muskop.session.tabDisplayNotes
import com.muskop.session.tabDisplayStringCount
import com.muskop.session.musicalMetadata
import com.muskop.session.resourceConnections
import com.muskop.session.ResourceOrganizationUpdate
import com.muskop.session.ResourceView
import com.muskop.session.organizedResources
import com.muskop.session.ResourceLearningUpdate
import com.muskop.session.ResourcePracticeSummary
import com.muskop.session.resourcePracticeSummary
import com.muskop.session.resourceStudyPath
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun LibraryScreen(
    session: SessionDocument,
    padding: PaddingValues,
    onSaveResource: (ResourceEditorInput) -> Unit,
    onDeleteResource: (String) -> Unit,
    onUpdateResourceLearning: (String, ResourceLearningUpdate) -> Unit,
    onUpdateResourceOrganization: (String, ResourceOrganizationUpdate) -> Unit,
    onImportResources: (ResourceImportBatch, (Result<Int>) -> Unit) -> Unit,
) {
    val resources = remember(session) { session.resources() }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var resourceHistory by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val viewerStates = rememberSaveableStateHolder()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingResource by rememberSaveable { mutableStateOf(false) }
    val selected = resources.firstOrNull { it.id == selectedId }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var showTags by rememberSaveable { mutableStateOf(false) }
    var resourceView by rememberSaveable { mutableStateOf(ResourceView.ACTIVE) }
    val listState = rememberLazyListState()
    var importOpen by rememberSaveable { mutableStateOf(false) }
    var importNotice by rememberSaveable { mutableStateOf<String?>(null) }
    if (importOpen) ResourceImportDialog(
        existingResources = resources, onImport = onImportResources,
        onClose = { importOpen = false },
        onImported = { count, inactive ->
            importOpen = false
            importNotice = "Imported $count resources." + if (inactive > 0) " $inactive are outside Active; use All resources to view them." else ""
            query = ""; selectedType = null; selectedTag = null; resourceView = ResourceView.ACTIVE
        },
    )

    if (creatingResource || editingId != null) {
        BackHandler { creatingResource = false; editingId = null }
        ResourceEditorScreen(
            resource = resources.firstOrNull { it.id == editingId },
            padding = padding,
            onSave = onSaveResource,
            onBack = { creatingResource = false; editingId = null },
        )
    } else if (selected != null) {
        key(selected.id) {
            viewerStates.SaveableStateProvider(selected.id) {
                ResourceViewer(
                    resource = selected,
                    practiceSummary = remember(session, selected.id) { session.resourcePracticeSummary(selected.id) },
                    resources = resources,
                    padding = padding,
                    backDescription = if (resourceHistory.isEmpty()) "Back to library" else "Back to previous resource",
                    onBack = {
                        selectedId = resourceHistory.lastOrNull()
                        resourceHistory = resourceHistory.dropLast(1)
                    },
                    onEdit = { editingId = it.id },
                    onDelete = {
                        onDeleteResource(selected.id)
                        selectedId = resourceHistory.lastOrNull { id -> id != selected.id && resources.any { it.id == id } }
                        resourceHistory = emptyList()
                    },
                    onUpdateLearning = { onUpdateResourceLearning(selected.id, it) },
                    onUpdateOrganization = { onUpdateResourceOrganization(selected.id, it) },
                    onOpenRelated = { target ->
                        val previousIndex = resourceHistory.indexOf(target.id)
                        resourceHistory = if (previousIndex >= 0) resourceHistory.take(previousIndex)
                            else resourceHistory + selected.id
                        selectedId = target.id
                    },
                )
            }
        }
    } else {
        ResourceList(
            resources = resources,
            padding = padding,
            onSelect = { resourceHistory = emptyList(); selectedId = it.id },
            onNewResource = { creatingResource = true },
            onImport = { importOpen = true },
            importNotice = importNotice,
            onDismissImportNotice = { importNotice = null },
            resourceView = resourceView,
            onViewChange = { resourceView = it },
            query = query,
            onQueryChange = { query = it },
            selectedType = selectedType,
            onTypeChange = { selectedType = it },
            selectedTag = selectedTag,
            onTagChange = { selectedTag = it },
            showTags = showTags,
            onShowTags = { showTags = !showTags },
            listState = listState,
        )
    }
}

@Composable
private fun ResourceList(
    resources: List<SessionResource>,
    padding: PaddingValues,
    onSelect: (SessionResource) -> Unit,
    onNewResource: () -> Unit,
    onImport: () -> Unit,
    importNotice: String?,
    onDismissImportNotice: () -> Unit,
    resourceView: ResourceView,
    onViewChange: (ResourceView) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedType: String?,
    onTypeChange: (String?) -> Unit,
    selectedTag: String?,
    onTagChange: (String?) -> Unit,
    showTags: Boolean,
    onShowTags: () -> Unit,
    listState: LazyListState,
) {
    val types = remember(resources) { resources.map(SessionResource::type).distinct().sorted() }
    val tags = remember(resources) { resources.flatMap(SessionResource::tags).distinct().sorted() }
    val filtered = remember(resources, query, selectedType, selectedTag, resourceView) {
        val term = query.trim()
        resources.organizedResources(resourceView).filter { resource ->
            val matchesQuery = term.isEmpty() ||
                listOf(resource.title, resource.category.orEmpty(), resourceTypeLabel(resource.type))
                    .plus(resource.tags).any { it.contains(term, ignoreCase = true) }
            val matchesType = selectedType == null || resource.type == selectedType
            val matchesTag = selectedTag == null || selectedTag in resource.tags
            matchesQuery && matchesType && matchesTag
        }
    }
    val hasFilters = query.isNotBlank() || selectedType != null || selectedTag != null || resourceView != ResourceView.ACTIVE
    fun clearFilters() { onQueryChange(""); onTypeChange(null); onTagChange(null); onViewChange(ResourceView.ACTIVE) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Library", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() })
                    Text("Your resources, ready to practice", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onImport) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = "Import resources")
                }
                FilledIconButton(onClick = onNewResource) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add resource")
                }
            }
        }
        importNotice?.let { notice -> item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(notice, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onDismissImportNotice) { Icon(Icons.Rounded.Close, contentDescription = "Dismiss import result") }
            }
        } }
        if (resources.isNotEmpty()) {
            item { ResourceViewPicker(resourceView, onViewChange) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search resources") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                )
            }
            if (types.size > 1 || selectedType != null) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = selectedType == null, onClick = { onTypeChange(null) },
                                label = { Text("All types") })
                        }
                        items(types, key = { it }) { type ->
                            FilterChip(selected = selectedType == type,
                                onClick = { onTypeChange(if (selectedType == type) null else type) },
                                label = { Text(resourceTypeLabel(type)) })
                        }
                    }
                }
            }
            if (tags.isNotEmpty()) {
                item {
                    TextButton(onClick = onShowTags,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                        Icon(Icons.Rounded.Tune, contentDescription = null)
                        Text(if (selectedTag != null) "Tags · $selectedTag" else if (showTags) "Hide tags" else "Filter by tag",
                            modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (showTags) {
                    item {
                        FilterRow(values = tags, selected = selectedTag,
                            onSelect = { onTagChange(if (selectedTag == it) null else it) })
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${filtered.size} ${if (filtered.size == 1) "resource" else "resources"}",
                        style = MaterialTheme.typography.titleMedium)
                    if (hasFilters) {
                        TextButton(onClick = ::clearFilters,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Clear search and filters") }
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (resources.isEmpty()) "Build your practice library" else if (!hasFilters) "No active resources" else "No matching resources",
                            style = MaterialTheme.typography.titleLarge)
                        Text(if (resources.isEmpty()) "Import a generated tablature or create a theory note from Add resource."
                            else "Use Hidden, Completed or All resources to review your other resources, or try another search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { if (resources.isEmpty()) onNewResource() else { clearFilters(); onViewChange(ResourceView.ALL) } }) {
                            Text(if (resources.isEmpty()) "Add resource" else "Show all resources")
                        }
                        if (resources.isEmpty()) TextButton(onClick = onImport) { Text("Import resources") }
                    }
                }
            }
        }
        items(filtered, key = SessionResource::id) { resource ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(resource) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(resource.title, style = MaterialTheme.typography.titleMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(resourceTypeLabel(resource.type), resource.category?.takeIf(String::isNotBlank))
                            .joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        resource.organizationLabel()?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        resource.learningSummaryLabel()?.let {
                            Text("Learning · $it", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (resource.tags.isNotEmpty()) {
                            Text(resource.tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

private fun resourceTypeLabel(type: String): String = when (type.uppercase(java.util.Locale.ROOT)) {
    "TAB" -> "Tablature"
    "THEORY" -> "Theory"
    "CHORD" -> "Chords"
    "SNIPPET" -> "Snippet"
    "COLLECTION" -> "Collection"
    "MEDIA" -> "Media"
    else -> type
}

@Composable
private fun FilterRow(values: List<String>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values, key = { it }) { value ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(value) })
        }
    }
}

@Composable
private fun ResourceViewer(
    resource: SessionResource,
    practiceSummary: ResourcePracticeSummary,
    resources: List<SessionResource>,
    padding: PaddingValues,
    backDescription: String,
    onBack: () -> Unit,
    onEdit: (SessionResource) -> Unit,
    onDelete: () -> Unit,
    onOpenRelated: (SessionResource) -> Unit,
    onUpdateLearning: (ResourceLearningUpdate) -> Unit,
    onUpdateOrganization: (ResourceOrganizationUpdate) -> Unit,
) {
    BackHandler(onBack = onBack)
    var confirmDelete by rememberSaveable(resource.id) { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete resource?") },
            text = { Text("This resource will move to the session trash. Routine blocks linked to it will lose their resource link; your practice history remains saved.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    var showAbout by rememberSaveable(resource.id) { mutableStateOf(false) }
    var resourceTab by rememberSaveable(resource.id) { mutableStateOf("read") }
    val metadata = remember(resource) { resource.musicalMetadata() }
    val connections = remember(resource, resources) { resourceConnections(resource, resources) }
    val connectionCount = connections.map { it.resource.id }.distinct().size
    val studyPath = remember(resource, resources) { resourceStudyPath(resource, resources) }
    val readState = rememberLazyListState()
    val connectionsState = rememberLazyListState()
    val pathState = rememberLazyListState()
    val readingStates = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backDescription)
            }
            Column(Modifier.weight(1f)) {
                Text(resource.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() })
                Text(listOfNotNull(resourceTypeLabel(resource.type), metadata?.difficulty?.let { "Difficulty ${it.level}/5" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onEdit(resource) }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit resource")
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete resource")
            }
        }
        ResourceLearningControl(resource, onUpdateLearning, Modifier.padding(horizontal = 20.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = resourceTab == "read", onClick = { resourceTab = "read" }, label = { Text("Read") })
            FilterChip(selected = resourceTab == "connections", onClick = { resourceTab = "connections" },
                label = { Text(if (connectionCount == 0) "Connections" else "Connections · $connectionCount") })
            FilterChip(selected = resourceTab == "path", onClick = { resourceTab = "path" }, label = { Text("Study path") })
        }
        LazyColumn(
            state = when (resourceTab) { "connections" -> connectionsState; "path" -> pathState; else -> readState },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "resource-organization") { ResourceOrganizationControl(resource, onUpdateOrganization) }
            if (resourceTab == "path") {
                resourceStudyPathItems(studyPath, onOpenRelated, onReadTarget = { resourceTab = "read" })
            } else if (resourceTab == "connections") {
                item(key = "resource-practice-summary") { ResourcePracticeSummaryPanel(practiceSummary) }
                resourceConnectionItems(metadata, connections, onOpenRelated)
            } else {
                item(key = "resource-reading") {
                    readingStates.SaveableStateProvider("reading") { ResourcePreview(resource) }
                }
                item(key = "resource-practice-summary") { ResourcePracticeSummaryPanel(practiceSummary) }
                item {
                    TextButton(onClick = { showAbout = !showAbout },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                        Text(if (showAbout) "Hide resource information" else "About this resource")
                    }
                }
                if (showAbout) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(resource.title, style = MaterialTheme.typography.titleMedium)
                            resource.category?.takeIf(String::isNotBlank)?.let { Text(it) }
                            if (resource.tags.isNotEmpty()) {
                                Text(resource.tags.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourcePreview(resource: SessionResource) {
    when {
        resource.type.equals("TAB", ignoreCase = true) -> {
            val sections = resource.tabSections()
            if (sections.isEmpty()) {
                Text("This tablature has no visible sections.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tabDocument = resource.content as? JsonObject
                    val signature = (tabDocument?.get("timeSignature") as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                    val tuning = (tabDocument?.get("tuning") as? JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }.orEmpty()
                    val documentBpm = (tabDocument?.get("baseBpm") as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                    var showZoom by rememberSaveable(resource.id) { mutableStateOf(false) }
                    var zoom by rememberSaveable(resource.id) { mutableFloatStateOf(1f) }
                    var overview by rememberSaveable(resource.id) { mutableStateOf(true) }
                    var readingScore by rememberSaveable(resource.id) { mutableStateOf(false) }
                    var selectedBlock by rememberSaveable(resource.id) { androidx.compose.runtime.mutableIntStateOf(0) }
                    if (readingScore) {
                        TabReadingSheet(title = resource.title, onClose = { readingScore = false }) { showDetails, scoreZoom ->
                            items(sections) { section ->
                                TabSectionViewer(section, signature, tuning, scoreZoom, documentBpm, showDetails)
                            }
                        }
                    }
                    TabDocumentDetails(resource.content)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !overview, onClick = { overview = false }, label = { Text("Practice") })
                            FilterChip(selected = overview, onClick = { overview = true }, label = { Text("Full resource") })
                        }
                        IconButton(onClick = { readingScore = true }) {
                            Icon(Icons.Rounded.Fullscreen, contentDescription = "Open full score for hands-free reading")
                        }
                        IconButton(onClick = { showZoom = !showZoom }) {
                            Icon(Icons.Rounded.ZoomIn, contentDescription = if (showZoom) "Hide zoom controls" else "Adjust tablature zoom")
                        }
                    }
                    if (showZoom) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(0.75f) }, enabled = zoom > 0.75f) {
                                Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out tablature")
                            }
                            TextButton(onClick = { zoom = 1f }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                                Text("Zoom ${(zoom * 100).toInt()}% · Reset")
                            }
                            IconButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(2f) }, enabled = zoom < 2f) {
                                Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in tablature")
                            }
                        }
                    }
                    if (overview) {
                        sections.forEach { section -> TabSectionViewer(section, signature, tuning, zoom, documentBpm) }
                    } else {
                        TabPracticeViewer(sections, signature, tuning, zoom, documentBpm, selectedBlock) { selectedBlock = it }
                    }
                }
            }
        }
        resource.type.equals("THEORY", ignoreCase = true) -> {
            SelectionContainer { Text(theoryBody(resource), style = MaterialTheme.typography.bodyLarge) }
        }
        else -> Text("This resource type is preserved and will receive its own viewer in a future iteration.")
    }
}

private fun sectionKindLabel(kind: String): String = when (kind.lowercase(java.util.Locale.ROOT)) {
    "tab" -> "Tablature"
    "arpeggio" -> "Arpeggio"
    "fingerstyle" -> "Fingerstyle"
    "chords" -> "Chords"
    else -> kind
}

@Composable
private fun StudyNotes(notes: String?) {
    if (notes.isNullOrBlank()) return
    var expanded by rememberSaveable(notes) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { expanded = !expanded },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
            Text(if (expanded) "Hide study notes" else "Study notes")
        }
        if (expanded) SelectionContainer { Text(notes, style = MaterialTheme.typography.bodyMedium) }
    }
}

private data class TabPracticeBlock(val section: TabSection, val measureIndex: Int?)

@Composable
private fun TabPracticeViewer(sections: List<TabSection>, signature: String?, tuning: List<String>, zoom: Float, documentBpm: String?, selectedBlock: Int, onSelectBlock: (Int) -> Unit) {
    val blocks = remember(sections) {
        sections.flatMap { section ->
            val measures = section.measures.indices.map { TabPracticeBlock(section, it) }
            if (section.chords.isNotEmpty() || measures.isEmpty()) measures + TabPracticeBlock(section, null) else measures
        }
    }
    if (blocks.isEmpty()) return
    var showBlockPicker by remember { mutableStateOf(false) }
    if (showBlockPicker) {
        AlertDialog(
            onDismissRequest = { showBlockPicker = false },
            title = { Text("Go to block") },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(blocks.size) { blockIndex ->
                        val item = blocks[blockIndex]
                        val description = item.measureIndex?.let { "Measure ${it + 1}" }
                            ?: if (item.section.chords.isNotEmpty()) "Chords" else "Section"
                        TextButton(onClick = { onSelectBlock(blockIndex); showBlockPicker = false }, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                            Text("${blockIndex + 1}. ${item.section.name} · $description", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBlockPicker = false },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Close") } },
        )
    }
    val index = selectedBlock.coerceIn(blocks.indices)
    val block = blocks[index]
    val section = block.section
    val measure = block.measureIndex?.let { section.measures[it] }
    val notes = remember(measure) { measure?.let(::tabDisplayNotes).orEmpty() }
    var showPima by rememberSaveable(section.id, section.name) { mutableStateOf(true) }
    val length = remember(signature) { tabMeasureLength(signature) }
    val stringCount = remember(section, tuning) {
        tabDisplayStringCount(tuning.size, section.measures.flatMap(::tabDisplayNotes).map { it.string })
    }
    Card(Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = { onSelectBlock(index - 1) }, enabled = index > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous block")
                }
                TextButton(onClick = { showBlockPicker = true }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text("Block ${index + 1} of ${blocks.size}", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = { onSelectBlock(index + 1) }, enabled = index < blocks.lastIndex) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next block")
                }
            }
            Text(section.name, style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(
                sectionKindLabel(section.kind),
                block.measureIndex?.let { "Measure ${it + 1}" },
                section.bpm?.let { "$it BPM" } ?: documentBpm?.let { "$it BPM" },
            ).joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (measure != null) {
                val measureNumber = requireNotNull(block.measureIndex) + 1
                PimaReadingOptions(notes, showPima, onShowPima = { showPima = it })
                if (length == null) {
                    Text("This time signature cannot be drawn yet. See the note details below.", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (tabDisplayNotes(measure).any { it.string !in 1..stringCount || it.beat < 1.0 || it.beat >= 1.0 + length }) {
                        Text("Some notes fall outside the diagram. See the details below.", style = MaterialTheme.typography.bodySmall)
                    }
                    androidx.compose.runtime.key(index) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val availableWidth = maxWidth
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TabMeasureLines(measure, measureNumber, length, stringCount, tuning, zoom, availableWidth, showPima)
                            }
                        }
                    }
                }
                androidx.compose.runtime.key(index) {
                    Column {
                        StudyNotes(section.notes)
                        TabMeasureDetails(measure, measureNumber, initiallyExpanded = length == null || tabDisplayNotes(measure).any {
                            it.string !in 1..stringCount || it.beat < 1.0 || it.beat >= 1.0 + length || (it.finger != null && it.pima == null)
                        })
                    }
                }
            } else {
                if (section.chords.isEmpty()) Text("This section has no measures.", style = MaterialTheme.typography.bodyMedium)
                section.chords.forEach { TabChordDetails(it) }
                StudyNotes(section.notes)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabSectionViewer(section: TabSection, signature: String?, tuning: List<String>, zoom: Float, documentBpm: String?, showDetails: Boolean = true) {
    val notes = remember(section) { section.measures.flatMap(::tabDisplayNotes) }
    var showPima by rememberSaveable(section.id, section.name) { mutableStateOf(true) }
    val stringCount = remember(section, tuning) { tabDisplayStringCount(tuning.size, section.measures.flatMap(::tabDisplayNotes).map { it.string }) }
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(section.name, style = MaterialTheme.typography.titleMedium)
            PimaReadingOptions(notes, showPima, onShowPima = { showPima = it })
            Text(listOfNotNull(sectionKindLabel(section.kind), section.bpm?.let { "$it BPM" } ?: documentBpm?.let { "$it BPM" })
                .joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val measureBeats = remember(signature) { tabMeasureLength(signature) }
            if (section.measures.isNotEmpty() && measureBeats == null) {
                Text("This time signature cannot be drawn yet. Note details are shown below.", style = MaterialTheme.typography.bodySmall)
            }
            if (measureBeats != null && section.measures.any { measure ->
                    tabDisplayNotes(measure).any { it.string !in 1..stringCount || it.beat < 1.0 || it.beat >= 1.0 + measureBeats }
                }) {
                Text("Some notes fall outside this diagram. Open the measure details to see them.", style = MaterialTheme.typography.bodySmall)
            }
            if (section.measures.isNotEmpty() && measureBeats != null) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val availableWidth = maxWidth
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        section.measures.forEachIndexed { index, measure ->
                            TabMeasureLines(measure, index + 1, measureBeats, stringCount, tuning, zoom,
                                availableWidth, showPima)
                        }
                    }
                }
            }
            if (showDetails) StudyNotes(section.notes)
            section.measures.forEachIndexed { index, measure ->
                val needsDetails = measureBeats == null || tabDisplayNotes(measure).any {
                    it.string !in 1..stringCount || it.beat < 1.0 || it.beat >= 1.0 + measureBeats || (it.finger != null && it.pima == null)
                }
                if (showDetails || needsDetails) {
                    TabMeasureDetails(measure, index + 1, initiallyExpanded = needsDetails)
                }
            }
            if (section.chords.isNotEmpty()) {
                section.chords.forEach { chord -> TabChordDetails(chord) }
            }
        }
    }
}

private fun theoryBody(resource: SessionResource): String = (resource.content as? JsonObject)
    ?.get("body")
    ?.jsonPrimitive
    ?.contentOrNull
    ?: "This theory resource has no text content."
