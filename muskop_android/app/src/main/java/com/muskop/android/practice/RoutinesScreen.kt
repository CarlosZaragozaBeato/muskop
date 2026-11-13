package com.muskop.android.practice

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.android.library.ResourcePreview
import com.muskop.session.ActivePracticeSnapshot
import com.muskop.session.ResourceLearningUpdate
import com.muskop.session.PracticeTime
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
import java.util.UUID
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
    onRecordPractice: (PracticeEntryInput, (Result<Unit>) -> Unit) -> Unit,
    onReviewResourceLearning: (String, ResourceLearningUpdate, (Result<Unit>) -> Unit) -> Unit,
    onSaveActive: (ActivePracticeSnapshot) -> Unit,
    onClearActive: () -> Unit,
    onSaveRoutine: (RoutineEditorInput) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    onPracticeModeChanged: (Boolean) -> Unit = {},
) {
    val routines = remember(session) { session.routines() }
    val resources = remember(session) { session.resources() }
    var returnedSnapshot by rememberSaveable { mutableStateOf<ActivePracticeSnapshot?>(null) }
    val persistedSnapshot = remember(session) { session.activePractice() }
    val stored = returnedSnapshot ?: persistedSnapshot
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingRoutine by rememberSaveable { mutableStateOf(false) }
    var practiceTarget by rememberSaveable { mutableStateOf<String?>(null) }

    var reviewEntry by rememberSaveable { mutableStateOf<PracticeEntryInput?>(null) }
    var reviewSnapshot by rememberSaveable { mutableStateOf<ActivePracticeSnapshot?>(null) }

    val inPractice = reviewEntry != null || !creatingRoutine && editingId == null &&
        (target == FREE_PRACTICE || (target != null && practiceTarget == target &&
            routines.any { it.id == target && it.blocks.isNotEmpty() }))
    LaunchedEffect(inPractice) { onPracticeModeChanged(inPractice) }
    DisposableEffect(Unit) { onDispose { onPracticeModeChanged(false) } }

    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            practiceTarget = null
            returnedSnapshot = null
            target = initialTarget
            onInitialTargetConsumed()
        }
    }
    reviewEntry?.let { entry ->
        PracticeSummaryScreen(
            session, entry, padding, onRecordPractice, onReviewResourceLearning,
            onDone = { reviewEntry = null; reviewSnapshot = null; returnedSnapshot = null },
            onReturnToPractice = {
                reviewSnapshot?.let { snapshot ->
                    returnedSnapshot = snapshot
                    target = if (snapshot.kind == "free") FREE_PRACTICE else snapshot.routineId
                    practiceTarget = snapshot.routineId
                }
                reviewEntry = null
                reviewSnapshot = null
            },
        )
        return
    }
    fun review(entry: PracticeEntryInput, snapshot: ActivePracticeSnapshot) {
        reviewEntry = entry
        reviewSnapshot = snapshot
        returnedSnapshot = null
        practiceTarget = null
        target = null
    }
    if (creatingRoutine || editingId != null) {
        BackHandler { creatingRoutine = false; editingId = null }
        RoutineEditorScreen(
            routine = routines.firstOrNull { it.id == editingId },
            resources = resources,
            padding = padding,
            onSave = onSaveRoutine,
            onDelete = { routineId ->
                onDeleteRoutine(routineId)
                practiceTarget = null
                target = null
            },
            onBack = { creatingRoutine = false; editingId = null },
        )
        return
    }
    when (target) {
        FREE_PRACTICE -> FreePracticeScreen(
            padding = padding,
            resources = resources,
            resume = stored?.takeIf { it.kind == "free" },
            onSave = ::review,
            onPersist = onSaveActive,
            onClear = onClearActive,
            onBack = { returnedSnapshot = null; target = null },
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
        )
        else -> {
            val routine = routines.firstOrNull { it.id == target }
            if (routine == null) {
                LaunchedEffect(target) { target = null }
            } else if (practiceTarget != routine.id || routine.blocks.isEmpty()) {
                RoutinePreparationScreen(
                    routine = routine,
                    resources = resources.associateBy(SessionResource::id),
                    resume = stored?.takeIf { it.kind == "routine" && it.routineId == routine.id },
                    padding = padding,
                    onStart = { practiceTarget = routine.id },
                    onEdit = { editingId = routine.id },
                    onBack = { returnedSnapshot = null; target = null },
                )
            } else {
                RoutinePracticeScreen(
                    routine = routine,
                    resources = resources.associateBy(SessionResource::id),
                    resume = stored?.takeIf { it.kind == "routine" && it.routineId == routine.id },
                    padding = padding,
                    onSave = ::review,
                    initiallyPaused = returnedSnapshot != null,
                    onPersist = onSaveActive,
                    onClear = onClearActive,
                    onBack = { returnedSnapshot = null; practiceTarget = null; target = null },
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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                ) {
                    Text("Routines", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() })
                    Text("Choose how you want to practice today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledIconButton(onClick = onCreateRoutine) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create routine")
                }
            }
        }
        active?.let { snapshot ->
            item {
                PendingPracticeCard(
                    snapshot = snapshot,
                    canResume = snapshot.kind == "free" ||
                        (snapshot.kind == "routine" && routines.any {
                            it.id == snapshot.routineId && it.blocks.isNotEmpty()
                        }),
                    onResume = onResume,
                    onDiscard = onDiscard,
                )
            }
        }
        item {
            Card(
                onClick = onStartFree,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text("Free practice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Play at your own pace", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }
        item {
            Text("Your routines", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
        }
        if (routines.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Build your first routine", style = MaterialTheme.typography.titleMedium)
                        Text("Organize your practice into blocks, at your own pace.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onCreateRoutine) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create routine")
                        }
                    }
                }
            }
        }
        items(routines, key = PracticeRoutine::id) { routine ->
            Card(
                onClick = { onStartRoutine(routine) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
                        Column(Modifier.weight(1f)) {
                            Text(routine.name, style = MaterialTheme.typography.titleMedium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${routine.blocks.size} blocks · ${routine.totalMinutes()} min",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                    routine.category?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    routine.description?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingPracticeCard(
    snapshot: ActivePracticeSnapshot,
    canResume: Boolean,
    onResume: (ActivePracticeSnapshot) -> Unit,
    onDiscard: () -> Unit,
) {
    val secondsLeft = snapshot.secondsLeft
    val detail = if (!canResume) {
        "This routine is unavailable or has no blocks. You can discard the saved practice."
    } else if (snapshot.kind == "routine" && secondsLeft != null) {
        "Block ${snapshot.blockIndex + 1} · ${formatDuration(secondsLeft)} remaining"
    } else {
        "${formatDuration(snapshot.elapsedBySkillSeconds.values.sum())} practiced"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Restore, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Continue where you left off", fontWeight = FontWeight.SemiBold)
                    Text(snapshot.routineName, style = MaterialTheme.typography.titleMedium)
                    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onResume(snapshot) },
                    enabled = canResume,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue practice") }
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
    onSave: (PracticeEntryInput, ActivePracticeSnapshot) -> Unit,
    onPersist: (ActivePracticeSnapshot) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    var skill by rememberSaveable { mutableStateOf(resume?.skill ?: "general") }
    var running by rememberSaveable { mutableStateOf(false) }
    var time by rememberSaveable { mutableStateOf(PracticeTime(resume?.elapsedBySkillSeconds.orEmpty(), resume?.resourceSeconds.orEmpty(), resume?.resourceLastPracticedAt.orEmpty())) }
    val practiceId = rememberSaveable { resume?.practiceId ?: UUID.randomUUID().toString() }
    val elapsed = time.skillSeconds.values.sum()
    var selectedResourceId by rememberSaveable { mutableStateOf(resume?.selectedResourceId) }
    var showResource by rememberSaveable { mutableStateOf(resume?.showResource ?: true) }
    val selectedResource = resources.firstOrNull { it.id == selectedResourceId }
    fun snapshot() = ActivePracticeSnapshot("free", FREE_PRACTICE, "Free practice", skill, 0, null, time.skillSeconds,
        practiceId, time.resourceSeconds, time.resourceLastPracticedAt, selectedResourceId, showResource)
    val latestSnapshot by rememberUpdatedState(snapshot())
    val persistOnDispose = remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            if (persistOnDispose.value && latestSnapshot.elapsedBySkillSeconds.values.sum() > 0) {
                onPersist(latestSnapshot)
            }
        }
    }
    LaunchedEffect(running, skill, selectedResource?.id) {
        while (running) {
            delay(1_000)
            if (!running) break
            time = time.tick(skill, selectedResource?.id)
        }
    }
    LaunchedEffect(skill, selectedResourceId, showResource) {
        if (elapsed > 0 && persistOnDispose.value) onPersist(snapshot())
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
            val minutes = max(1, elapsed / 60)
            onSave(
                PracticeEntryInput(
                    todayKey(), FREE_PRACTICE, "Free practice", minutes, false,
                    distributeMinutes(time.skillSeconds, minutes),
                    seconds = elapsed,
                    id = practiceId, resourceSeconds = time.resourceSeconds, resourceLastPracticedAt = time.resourceLastPracticedAt,
                ),
                snapshot(),
            )
        } else {
            onClear()
            onBack()
        }
    }

    PausePracticeOnBackground {
        running = false
        if (persistOnDispose.value && time.totalSeconds > 0) onPersist(snapshot())
    }
    BackHandler(onBack = ::leavePending)
    PracticeLayout(
        padding = padding,
        title = "Free practice",
        subtitle = if (running) "Practicing" else if (elapsed > 0) "Paused" else "Ready",
        onBack = ::leavePending,
        controls = {
            MetronomePanel()
            PracticeControls(
                timer = formatDuration(elapsed),
                status = "Practice time",
                running = running,
                primaryLabel = if (running) "Pause" else if (elapsed > 0) "Continue" else "Start",
                onToggle = { running = !running; onPersist(snapshot()) },
                onFinish = ::finishPractice,
                finishEnabled = elapsed > 0,
            )
        },
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier,
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selectedResource == null) {
                item {
                    PracticeFocusPanel(
                        time = formatDuration(elapsed),
                        label = "Practice time",
                        status = when {
                            running -> "Practicing · ${practiceSkillLabel(skill)}"
                            elapsed > 0 -> "Paused · ${practiceSkillLabel(skill)}"
                            else -> "Ready · ${practiceSkillLabel(skill)}"
                        },
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What are you practicing?", style = MaterialTheme.typography.titleMedium)
                    SkillPicker(skill) { skill = it }
                }
            }
            item {
                PracticeResourceChooser(
                    resources = resources,
                    selected = selectedResource,
                    onSelect = { selectedResourceId = it?.id },
                )
            }
            selectedResource?.let { resource ->
                item {
                    TextButton(onClick = { showResource = !showResource }) {
                        Text(if (showResource) "Hide resource · practice from memory" else "Show resource")
                    }
                }
                if (showResource) item(key = resource.id) { ResourcePreview(resource) }
                else item { Text("Time is still recorded for ${resource.title}.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (selectedResource == null && selectedResourceId != null) item {
                Text("Previously selected resource unavailable. Time will remain unassigned until you choose a resource.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RoutinePracticeScreen(
    routine: PracticeRoutine,
    resources: Map<String, SessionResource>,
    resume: ActivePracticeSnapshot?,
    padding: PaddingValues,
    initiallyPaused: Boolean = false,
    onSave: (PracticeEntryInput, ActivePracticeSnapshot) -> Unit,
    onPersist: (ActivePracticeSnapshot) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    var blockIndex by rememberSaveable(routine.id) {
        mutableStateOf((resume?.blockIndex ?: 0).coerceIn(0, routine.blocks.lastIndex))
    }
    var running by rememberSaveable {
        mutableStateOf(!initiallyPaused && (resume?.let {
            !(it.secondsLeft == 0 && it.blockIndex >= routine.blocks.lastIndex)
        } ?: true))
    }
    var secondsLeft by rememberSaveable(routine.id) {
        mutableStateOf(resume?.secondsLeft ?: routine.blocks[blockIndex].minutes * 60)
    }
    var time by rememberSaveable(routine.id) { mutableStateOf(PracticeTime(resume?.elapsedBySkillSeconds.orEmpty(), resume?.resourceSeconds.orEmpty(), resume?.resourceLastPracticedAt.orEmpty())) }
    val practiceId = rememberSaveable(routine.id) { resume?.practiceId ?: UUID.randomUUID().toString() }
    var finished by rememberSaveable {
        mutableStateOf(
            resume?.let { snapshot ->
                snapshot.secondsLeft == 0 &&
                    snapshot.blockIndex.coerceIn(0, routine.blocks.lastIndex) == routine.blocks.lastIndex
            } == true,
        )
    }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    val block = routine.blocks[blockIndex]
    fun snapshot() = ActivePracticeSnapshot(
        "routine", routine.id, routine.name, null, blockIndex, secondsLeft, time.skillSeconds,
        practiceId, time.resourceSeconds, time.resourceLastPracticedAt,
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
    LaunchedEffect(running, blockIndex, finished) {
        while (running && !finished) {
            delay(1_000)
            if (!running || finished) break
            val skill = routine.blocks[blockIndex].skill ?: "general"
            time = time.tick(skill, routine.blocks[blockIndex].resourceId?.takeIf { resources.containsKey(it) })
            if (secondsLeft <= 1) {
                if (blockIndex == routine.blocks.lastIndex) {
                    secondsLeft = 0
                    running = false
                    finished = true
                } else {
                    blockIndex += 1
                    secondsLeft = routine.blocks[blockIndex].minutes * 60
                }
            } else {
                secondsLeft -= 1
            }
        }
    }
    LaunchedEffect(time.skillSeconds, blockIndex, secondsLeft) {
        val elapsed = time.skillSeconds.values.sum()
        if (elapsed > 0 && elapsed % 15 == 0) onPersist(snapshot())
    }
    LaunchedEffect(blockIndex) {
        if (time.totalSeconds > 0 && persistOnDispose.value) onPersist(snapshot())
    }

    fun leavePending() {
        running = false
        persistOnDispose.value = false
        if (time.skillSeconds.isNotEmpty()) onPersist(snapshot()) else onClear()
        onBack()
    }
    fun finishPractice(completed: Boolean) {
        running = false
        persistOnDispose.value = false
        val totalSeconds = time.skillSeconds.values.sum()
        if (totalSeconds > 0) {
            val minutes = max(1, totalSeconds / 60)
            onSave(
                PracticeEntryInput(
                    todayKey(), routine.id, routine.name, minutes, completed,
                    distributeMinutes(time.skillSeconds, minutes),
                    seconds = totalSeconds,
                    id = practiceId, resourceSeconds = time.resourceSeconds, resourceLastPracticedAt = time.resourceLastPracticedAt,
                ),
                snapshot(),
            )
        } else {
            onClear()
            onBack()
        }
    }

    PausePracticeOnBackground {
        running = false
        if (persistOnDispose.value && time.totalSeconds > 0) onPersist(snapshot())
    }
    BackHandler(onBack = ::leavePending)
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Save partial practice?") },
            text = { Text("Your practiced time will be saved. This routine will remain incomplete.") },
            confirmButton = {
                TextButton(onClick = { showFinishDialog = false; finishPractice(false) }) { Text("Save practice") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Keep practicing") }
            },
        )
    }
    PracticeLayout(
        padding = padding,
        title = routine.name,
        subtitle = "Block ${blockIndex + 1} of ${routine.blocks.size}",
        onBack = ::leavePending,
        controls = {
            MetronomePanel(initialBpm = block.bpm)
            PracticeControls(
                timer = formatDuration(secondsLeft),
                status = if (finished) "Complete" else if (running) "Remaining" else "Paused",
                running = running,
                primaryLabel = if (finished) "Save practice" else if (running) "Pause" else "Continue",
                onToggle = { running = !running; onPersist(snapshot()) },
                onFinish = {
                    if (finished) finishPractice(true) else {
                        running = false
                        onPersist(snapshot())
                        showFinishDialog = true
                    }
                },
                finishEnabled = time.skillSeconds.values.sum() > 0,
                finished = finished,
                onPrevious = {
                    if (blockIndex > 0) {
                        blockIndex -= 1
                        secondsLeft = routine.blocks[blockIndex].minutes * 60
                    }
                },
                previousEnabled = blockIndex > 0,
                onNext = {
                    if (blockIndex == routine.blocks.lastIndex) {
                        running = false
                        secondsLeft = 0
                        finished = true
                    } else {
                        blockIndex += 1
                        secondsLeft = routine.blocks[blockIndex].minutes * 60
                    }
                },
                nextDescription = if (blockIndex == routine.blocks.lastIndex) "Complete routine" else "Next block",
            )
        },
    ) { contentModifier ->
        // Reset scroll and resource-local UI when the practice block changes.
        key(blockIndex) {
            LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(block.name, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                }
                val resource = block.resourceId?.let(resources::get)
                if (resource == null) {
                    item {
                        PracticeFocusPanel(
                            time = formatDuration(secondsLeft),
                            label = "Time remaining",
                            status = if (finished) "Complete" else if (running) "Practicing" else "Paused",
                            progress = if (block.minutes > 0) 1f - secondsLeft.toFloat() / (block.minutes * 60) else null,
                        )
                    }
                }
                if (resource != null) {
                    item { ResourcePreview(resource) }
                } else if (block.resourceId != null) {
                    item {
                        Text("Linked resource unavailable. You can continue with the block instructions.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (block.notes.isNullOrBlank()) {
                    item {
                        Text("Practice this block at your own pace.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                block.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Instructions", style = MaterialTheme.typography.titleMedium)
                            Text(notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillPicker(selected: String, onSelect: (String) -> Unit) {
    val skills = practiceSkills
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skills.forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

private val practiceSkills = listOf(
    "general" to "General", "arpegios" to "Arpeggios", "cambios-acordes" to "Chord changes",
    "patrones" to "Patterns", "velocidad" to "Speed", "tecnica" to "Technique",
    "repertorio" to "Repertoire",
)

private fun practiceSkillLabel(skill: String): String = practiceSkills.firstOrNull { it.first == skill }?.second ?: skill

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
