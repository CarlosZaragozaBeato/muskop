package com.muskop.android.practice

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.android.library.ResourcePreview
import com.muskop.session.ActivePracticeSnapshot
import com.muskop.session.PracticeEntryInput
import com.muskop.session.PracticeRoutine
import com.muskop.session.RoutineEditorInput
import com.muskop.session.SessionDocument
import com.muskop.session.SessionResource
import com.muskop.session.activePractice
import com.muskop.session.resources
import com.muskop.session.routines
import com.muskop.session.totalMinutes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.max

private const val FREE_PRACTICE = "free"

@Composable
fun RoutinesScreen(
    session: SessionDocument,
    padding: PaddingValues,
    initialTarget: String? = null,
    onInitialTargetConsumed: () -> Unit = {},
    onRecordPractice: (PracticeEntryInput) -> Unit,
    onSaveActive: (ActivePracticeSnapshot) -> Unit,
    onClearActive: () -> Unit,
    onSaveRoutine: (RoutineEditorInput) -> Unit,
) {
    val routines = remember(session) { session.routines() }
    val resources = remember(session) { session.resources() }
    val stored = remember(session) { session.activePractice() }
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingRoutine by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            target = initialTarget
            onInitialTargetConsumed()
        }
    }
    if (creatingRoutine || editingId != null) {
        RoutineEditorScreen(
            routine = routines.firstOrNull { it.id == editingId },
            resources = resources,
            padding = padding,
            onSave = onSaveRoutine,
            onBack = { creatingRoutine = false; editingId = null },
        )
        return
    }
    when (target) {
        FREE_PRACTICE -> FreePracticeScreen(
            padding = padding,
            resources = resources,
            resume = stored?.takeIf { it.kind == "free" },
            onSave = onRecordPractice,
            onPersist = onSaveActive,
            onClear = onClearActive,
            onBack = { target = null },
        )
        null -> RoutineList(
            routines = routines,
            active = stored,
            padding = padding,
            onStartRoutine = { target = it.id },
            onStartFree = { target = FREE_PRACTICE },
            onResume = { target = if (it.kind == "free") FREE_PRACTICE else it.routineId },
            onDiscard = onClearActive,
            onCreateRoutine = { creatingRoutine = true },
            onEditRoutine = { editingId = it.id },
        )
        else -> {
            val routine = routines.firstOrNull { it.id == target }
            if (routine == null) {
                LaunchedEffect(target) { target = null }
            } else {
                RoutinePracticeScreen(
                    routine = routine,
                    resources = resources.associateBy(SessionResource::id),
                    resume = stored?.takeIf { it.kind == "routine" && it.routineId == routine.id },
                    padding = padding,
                    onSave = onRecordPractice,
                    onPersist = onSaveActive,
                    onClear = onClearActive,
                    onBack = { target = null },
                )
            }
        }
    }
}

@Composable
private fun RoutineList(
    routines: List<PracticeRoutine>,
    active: ActivePracticeSnapshot?,
    padding: PaddingValues,
    onStartRoutine: (PracticeRoutine) -> Unit,
    onStartFree: () -> Unit,
    onResume: (ActivePracticeSnapshot) -> Unit,
    onDiscard: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (PracticeRoutine) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Routines", style = MaterialTheme.typography.headlineMedium)
                    Text("Choose how you want to practice today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onCreateRoutine) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }
        active?.let { snapshot ->
            item { PendingPracticeCard(snapshot, onResume, onDiscard) }
        }
        item {
            Card(
                onClick = onStartFree,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text("Free practice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Timer, resources and metronome at your own pace")
                    }
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Start")
                }
            }
        }
        item { Text("Your routines", style = MaterialTheme.typography.titleLarge) }
        if (routines.isEmpty()) {
            item { Text("You do not have any routines yet. Create one to organize your practice.") }
        }
        items(routines, key = PracticeRoutine::id) { routine ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${routine.blocks.size} blocks · ${routine.totalMinutes()} min",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { onStartRoutine(routine) },
                            enabled = routine.blocks.isNotEmpty(),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Start routine")
                        }
                        IconButton(onClick = { onEditRoutine(routine) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit routine")
                        }
                    }
                    routine.category?.takeIf(String::isNotBlank)?.let {
                        Text(it.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    routine.description?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingPracticeCard(
    snapshot: ActivePracticeSnapshot,
    onResume: (ActivePracticeSnapshot) -> Unit,
    onDiscard: () -> Unit,
) {
    val secondsLeft = snapshot.secondsLeft
    val detail = if (snapshot.kind == "routine" && secondsLeft != null) {
        "Block ${snapshot.blockIndex + 1} · ${formatDuration(secondsLeft)} remaining"
    } else {
        "${formatDuration(snapshot.elapsedBySkillSeconds.values.sum())} practiced"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Continue where you left off", fontWeight = FontWeight.SemiBold)
                    Text(snapshot.routineName, style = MaterialTheme.typography.titleMedium)
                    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onResume(snapshot) }) { Text("Continue") }
                TextButton(onClick = onDiscard) { Text("Discard") }
            }
        }
    }
}

@Composable
private fun FreePracticeScreen(
    padding: PaddingValues,
    resources: List<SessionResource>,
    resume: ActivePracticeSnapshot?,
    onSave: (PracticeEntryInput) -> Unit,
    onPersist: (ActivePracticeSnapshot) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    var skill by rememberSaveable { mutableStateOf(resume?.skill ?: "general") }
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsedBySkill by remember { mutableStateOf(resume?.elapsedBySkillSeconds ?: emptyMap()) }
    val elapsed = elapsedBySkill.values.sum()
    fun snapshot() = ActivePracticeSnapshot("free", FREE_PRACTICE, "Free practice", skill, 0, null, elapsedBySkill)
    val latestSnapshot by rememberUpdatedState(snapshot())
    val persistOnDispose = remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            if (persistOnDispose.value && latestSnapshot.elapsedBySkillSeconds.values.sum() > 0) {
                onPersist(latestSnapshot)
            }
        }
    }
    LaunchedEffect(running, skill) {
        while (running) {
            delay(1_000)
            elapsedBySkill = elapsedBySkill + (skill to ((elapsedBySkill[skill] ?: 0) + 1))
        }
    }
    LaunchedEffect(elapsed) {
        if (elapsed > 0 && elapsed % 15 == 0) onPersist(snapshot())
    }

    fun leavePending() {
        running = false
        persistOnDispose.value = false
        if (elapsed > 0) onPersist(snapshot()) else onClear()
        onBack()
    }
    fun finishPractice() {
        running = false
        persistOnDispose.value = false
        if (elapsed > 0) {
            val minutes = max(1, (elapsed + 59) / 60)
            onSave(
                PracticeEntryInput(
                    todayKey(), FREE_PRACTICE, "Free practice", minutes, false,
                    distributeMinutes(elapsedBySkill, minutes),
                ),
            )
        } else onClear()
        onBack()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = ::leavePending) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                Text("Leave and continue later")
            }
        }
        item { Text("Free practice", style = MaterialTheme.typography.headlineMedium) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What are you practicing?", style = MaterialTheme.typography.titleMedium)
                SkillPicker(skill) { skill = it }
            }
        }
        item { PracticeTimer(elapsed) }
        item {
            Button(
                onClick = {
                    running = !running
                    onPersist(snapshot())
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (running) "Pause" else if (elapsed > 0) "Continue" else "Start") }
        }
        item { MetronomePanel() }
        if (resources.isNotEmpty()) {
            item { PracticeResources(resources) }
        }
        item {
            FilledTonalButton(
                onClick = ::finishPractice,
                enabled = elapsed > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Finish and save") }
        }
    }
}

@Composable
private fun RoutinePracticeScreen(
    routine: PracticeRoutine,
    resources: Map<String, SessionResource>,
    resume: ActivePracticeSnapshot?,
    padding: PaddingValues,
    onSave: (PracticeEntryInput) -> Unit,
    onPersist: (ActivePracticeSnapshot) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    var blockIndex by remember(routine.id) {
        mutableStateOf((resume?.blockIndex ?: 0).coerceIn(0, routine.blocks.lastIndex))
    }
    var running by rememberSaveable { mutableStateOf(false) }
    var secondsLeft by remember(routine.id) {
        mutableStateOf(resume?.secondsLeft ?: routine.blocks[blockIndex].minutes * 60)
    }
    var elapsedBySkill by remember(routine.id) { mutableStateOf(resume?.elapsedBySkillSeconds ?: emptyMap()) }
    var finished by rememberSaveable {
        mutableStateOf(
            resume?.let { snapshot ->
                snapshot.secondsLeft == 0 &&
                    snapshot.blockIndex.coerceIn(0, routine.blocks.lastIndex) == routine.blocks.lastIndex
            } == true,
        )
    }
    var showResource by rememberSaveable { mutableStateOf(false) }
    val block = routine.blocks[blockIndex]
    fun snapshot() = ActivePracticeSnapshot(
        "routine", routine.id, routine.name, null, blockIndex, secondsLeft, elapsedBySkill,
    )
    val latestSnapshot by rememberUpdatedState(snapshot())
    val persistOnDispose = remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            if (persistOnDispose.value && latestSnapshot.elapsedBySkillSeconds.values.sum() > 0) {
                onPersist(latestSnapshot)
            }
        }
    }
    LaunchedEffect(blockIndex) {
        if (resume == null || blockIndex != resume.blockIndex) {
            secondsLeft = routine.blocks[blockIndex].minutes * 60
        }
    }
    LaunchedEffect(running, blockIndex, finished) {
        while (running && !finished) {
            delay(1_000)
            val skill = routine.blocks[blockIndex].skill ?: "general"
            elapsedBySkill = elapsedBySkill + (skill to ((elapsedBySkill[skill] ?: 0) + 1))
            if (secondsLeft <= 1) {
                if (blockIndex == routine.blocks.lastIndex) {
                    secondsLeft = 0
                    running = false
                    finished = true
                } else {
                    blockIndex += 1
                }
            } else {
                secondsLeft -= 1
            }
        }
    }
    LaunchedEffect(elapsedBySkill, blockIndex, secondsLeft) {
        val elapsed = elapsedBySkill.values.sum()
        if (elapsed > 0 && elapsed % 15 == 0) onPersist(snapshot())
    }

    fun leavePending() {
        running = false
        persistOnDispose.value = false
        if (elapsedBySkill.isNotEmpty()) onPersist(snapshot()) else onClear()
        onBack()
    }
    fun finishPractice(completed: Boolean) {
        running = false
        persistOnDispose.value = false
        val totalSeconds = elapsedBySkill.values.sum()
        if (totalSeconds > 0) {
            val minutes = max(1, (totalSeconds + 59) / 60)
            onSave(
                PracticeEntryInput(
                    todayKey(), routine.id, routine.name, minutes, completed,
                    distributeMinutes(elapsedBySkill, minutes),
                ),
            )
        } else onClear()
        onBack()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = ::leavePending) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                Text("Leave and continue later")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(routine.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Block ${blockIndex + 1} of ${routine.blocks.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(block.name, style = MaterialTheme.typography.titleLarge)
                    block.notes?.takeIf(String::isNotBlank)?.let { Text(it) }
                    block.resourceId?.let { id ->
                        val resource = resources[id]
                        if (resource != null) {
                            TextButton(onClick = { showResource = !showResource }) {
                                Text(if (showResource) "Hide ${resource.title}" else "View ${resource.title}")
                            }
                        }
                    }
                }
            }
        }
        if (showResource) {
            val resource = block.resourceId?.let(resources::get)
            if (resource != null) item { ResourcePreview(resource) }
        }
        item { PracticeTimer(block.minutes * 60 - secondsLeft, secondsLeft) }
        item { MetronomePanel(initialBpm = block.bpm) }
        if (finished) {
            item { Text("Routine complete", style = MaterialTheme.typography.titleLarge) }
            item {
                Button(onClick = { finishPractice(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save completed routine")
                }
            }
        } else {
            item {
                Button(
                    onClick = {
                        running = !running
                        onPersist(snapshot())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (running) "Pause" else if (elapsedBySkill.isNotEmpty()) "Continue" else "Start") }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { if (blockIndex > 0) blockIndex -= 1 },
                        enabled = blockIndex > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Previous")
                    }
                    FilledTonalButton(
                        onClick = {
                            if (blockIndex == routine.blocks.lastIndex) {
                                running = false
                                finished = true
                            } else blockIndex += 1
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
            item {
                TextButton(
                    onClick = { finishPractice(false) },
                    enabled = elapsedBySkill.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Finish and save partial practice") }
            }
        }
    }
}

@Composable
private fun PracticeResources(resources: List<SessionResource>) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = resources.firstOrNull { it.id == selectedId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Practice resources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(resources, key = SessionResource::id) { resource ->
                    FilterChip(
                        selected = selectedId == resource.id,
                        onClick = { selectedId = if (selectedId == resource.id) null else resource.id },
                        label = { Text(resource.title) },
                    )
                }
            }
            if (selected != null) ResourcePreview(selected)
        }
    }
}

@Composable
private fun SkillPicker(selected: String, onSelect: (String) -> Unit) {
    val skills = listOf(
        "general" to "General", "arpegios" to "Arpeggios", "cambios-acordes" to "Chord changes",
        "patrones" to "Patterns", "velocidad" to "Speed", "tecnica" to "Technique",
        "repertorio" to "Repertoire",
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skills.forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

@Composable
private fun PracticeTimer(elapsedSeconds: Int, remainingSeconds: Int? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(formatDuration(remainingSeconds ?: elapsedSeconds), style = MaterialTheme.typography.displayMedium)
        Text(if (remainingSeconds == null) "Practice time" else "Time remaining")
    }
}

private fun formatDuration(seconds: Int): String {
    val normalized = seconds.coerceAtLeast(0)
    return "${normalized / 60}:${(normalized % 60).toString().padStart(2, '0')}"
}

private fun distributeMinutes(secondsBySkill: Map<String, Int>, totalMinutes: Int): Map<String, Int> {
    val totalSeconds = secondsBySkill.values.sum().coerceAtLeast(1)
    val base = secondsBySkill.mapValues { (_, seconds) -> seconds * totalMinutes / totalSeconds }.toMutableMap()
    var remaining = totalMinutes - base.values.sum()
    val byRemainder = secondsBySkill.keys.sortedByDescending { skill ->
        (secondsBySkill.getValue(skill) * totalMinutes) % totalSeconds
    }
    var index = 0
    while (remaining > 0 && byRemainder.isNotEmpty()) {
        val skill = byRemainder[index % byRemainder.size]
        base[skill] = base.getValue(skill) + 1
        remaining -= 1
        index += 1
    }
    return base.filterValues { it > 0 }
}

private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
