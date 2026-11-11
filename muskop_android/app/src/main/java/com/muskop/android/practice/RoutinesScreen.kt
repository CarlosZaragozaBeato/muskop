package com.muskop.android.practice

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        FREE_PRACTICE -> FreePracticeScreen(padding, stored?.takeIf { it.kind == "free" }, onRecordPractice, onSaveActive, onClearActive) { target = null }
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
                RoutinePracticeScreen(routine, resources.associateBy(SessionResource::id), stored?.takeIf { it.kind == "routine" && it.routineId == routine.id }, padding, onRecordPractice, onSaveActive, onClearActive) { target = null }
            }
        }
    }
}

@Composable
private fun RoutineList(
    routines: List<PracticeRoutine>, active: ActivePracticeSnapshot?, padding: PaddingValues,
    onStartRoutine: (PracticeRoutine) -> Unit, onStartFree: () -> Unit,
    onResume: (ActivePracticeSnapshot) -> Unit, onDiscard: () -> Unit,
    onCreateRoutine: () -> Unit, onEditRoutine: (PracticeRoutine) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Rutinas", style = MaterialTheme.typography.headlineMedium) }
        active?.let { snapshot -> item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Práctica pendiente", fontWeight = FontWeight.SemiBold)
                Text(snapshot.routineName)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onResume(snapshot) }) { Text("Reanudar") }
                    TextButton(onClick = onDiscard) { Text("Descartar") }
                }
            } }
        } }
        item { TextButton(onClick = onCreateRoutine) { Text("+ Nueva rutina") } }
        item { Button(onClick = onStartFree, modifier = Modifier.fillMaxWidth()) { Text("Iniciar práctica libre") } }
        if (routines.isEmpty()) item { Text("Aún no hay rutinas en esta sesión.") }
        items(routines, key = PracticeRoutine::id) { routine ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(routine.name, fontWeight = FontWeight.SemiBold)
                routine.description?.let { Text(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    routine.category?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    AssistChip(onClick = {}, label = { Text("${routine.blocks.size} bloques · ${routine.totalMinutes()} min") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onStartRoutine(routine) }, enabled = routine.blocks.isNotEmpty()) { Text("Practicar") }
                    TextButton(onClick = { onEditRoutine(routine) }) { Text("Editar") }
                }
            } }
        }
    }
}

@Composable
private fun FreePracticeScreen(
    padding: PaddingValues, resume: ActivePracticeSnapshot?, onSave: (PracticeEntryInput) -> Unit,
    onPersist: (ActivePracticeSnapshot) -> Unit, onClear: () -> Unit, onBack: () -> Unit,
) {
    var skill by remember { mutableStateOf(resume?.skill ?: "general") }
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(resume?.elapsedBySkillSeconds?.values?.sum() ?: 0) }
    fun snapshot() = ActivePracticeSnapshot("free", FREE_PRACTICE, "Free practice", skill, 0, null, mapOf(skill to elapsed))
    LaunchedEffect(running) { while (running) { delay(1_000); elapsed += 1 } }
    LaunchedEffect(elapsed, skill) { if (elapsed > 0 && elapsed % 15 == 0) onPersist(snapshot()) }
    fun saveAndExit() {
        if (elapsed > 0) {
            val minutes = max(1, (elapsed + 59) / 60)
            onSave(PracticeEntryInput(todayKey(), FREE_PRACTICE, "Free practice", minutes, false, mapOf(skill to minutes)))
        } else onClear()
        onBack()
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { running = false; saveAndExit() }) { Text("← Volver") }
        Text("Práctica libre", style = MaterialTheme.typography.headlineMedium)
        SkillPicker(skill) { skill = it }
        PracticeTimer(elapsed)
        Button(onClick = { running = !running; if (running) onPersist(snapshot()) }, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Pausar" else "Iniciar") }
        Button(onClick = { running = false; saveAndExit() }, enabled = elapsed > 0, modifier = Modifier.fillMaxWidth()) { Text("Guardar práctica") }
    }
}

@Composable
private fun RoutinePracticeScreen(
    routine: PracticeRoutine, resources: Map<String, SessionResource>, resume: ActivePracticeSnapshot?, padding: PaddingValues,
    onSave: (PracticeEntryInput) -> Unit, onPersist: (ActivePracticeSnapshot) -> Unit, onClear: () -> Unit, onBack: () -> Unit,
) {
    var blockIndex by remember(routine.id) { mutableStateOf((resume?.blockIndex ?: 0).coerceIn(0, routine.blocks.lastIndex)) }
    var running by rememberSaveable { mutableStateOf(false) }
    var secondsLeft by remember(routine.id) { mutableStateOf(resume?.secondsLeft ?: routine.blocks[blockIndex].minutes * 60) }
    var elapsedBySkill by remember(routine.id) { mutableStateOf(resume?.elapsedBySkillSeconds ?: emptyMap()) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var showResource by rememberSaveable { mutableStateOf(false) }
    var metronome by rememberSaveable { mutableStateOf(false) }
    var beat by remember { mutableStateOf(0) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }
    val block = routine.blocks[blockIndex]
    val blockBpm = block.bpm
    fun snapshot() = ActivePracticeSnapshot("routine", routine.id, routine.name, null, blockIndex, secondsLeft, elapsedBySkill)
    DisposableEffect(tone) { onDispose { tone.release() } }
    LaunchedEffect(blockIndex) { if (resume == null || blockIndex != resume.blockIndex) secondsLeft = routine.blocks[blockIndex].minutes * 60; beat = 0 }
    LaunchedEffect(running, blockIndex, finished) {
        while (running && !finished) {
            delay(1_000)
            val skill = routine.blocks[blockIndex].skill ?: "general"
            elapsedBySkill = elapsedBySkill + (skill to (elapsedBySkill[skill] ?: 0) + 1)
            if (secondsLeft <= 1) {
                if (blockIndex == routine.blocks.lastIndex) { running = false; finished = true } else blockIndex += 1
            } else secondsLeft -= 1
        }
    }
    LaunchedEffect(elapsedBySkill, blockIndex, secondsLeft) { if (elapsedBySkill.values.sum() > 0 && elapsedBySkill.values.sum() % 15 == 0) onPersist(snapshot()) }
    LaunchedEffect(metronome, running, blockBpm) {
        if (metronome && running && blockBpm != null) while (metronome && running) {
            beat = (beat % 4) + 1
            tone.startTone(if (beat == 1) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, 60)
            delay((60_000L / blockBpm).coerceAtLeast(1L))
        }
    }
    fun saveAndExit(completed: Boolean) {
        val totalSeconds = elapsedBySkill.values.sum()
        if (totalSeconds > 0) {
            val minutes = max(1, (totalSeconds + 59) / 60)
            onSave(PracticeEntryInput(todayKey(), routine.id, routine.name, minutes, completed, distributeMinutes(elapsedBySkill, minutes)))
        } else onClear()
        onBack()
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { running = false; metronome = false; saveAndExit(false) }) { Text("← Salir") }
        Text(routine.name, style = MaterialTheme.typography.headlineSmall)
        Text("Bloque ${blockIndex + 1} de ${routine.blocks.size}")
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(block.name, style = MaterialTheme.typography.titleLarge)
            block.notes?.let { Text(it) }
            block.resourceId?.let { id ->
                val resource = resources[id]
                Text("Recurso: ${resource?.title ?: "no disponible"}")
                if (resource != null) TextButton(onClick = { showResource = !showResource }) { Text(if (showResource) "Ocultar recurso" else "Ver recurso") }
            }
            Text(block.bpm?.let { "$it BPM" } ?: "Tempo libre")
        } }
        if (showResource) {
            val resource = block.resourceId?.let(resources::get)
            if (resource != null) ResourcePreview(resource)
        }
        PracticeTimer(block.minutes * 60 - secondsLeft, secondsLeft)
        if (block.bpm != null) Button(onClick = { metronome = !metronome }, modifier = Modifier.fillMaxWidth()) {
            Text(if (metronome) "Metrónomo activado · pulso $beat" else "Activar metrónomo · ${block.bpm} BPM")
        }
        if (finished) {
            Text("Rutina terminada. Guarda el registro cuando estés listo.")
            Button(onClick = { saveAndExit(true) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar rutina completada") }
        } else {
            Button(onClick = { running = !running; if (running) onPersist(snapshot()) }, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Pausar" else "Iniciar") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (blockIndex > 0) blockIndex -= 1 }, enabled = blockIndex > 0, modifier = Modifier.weight(1f)) { Text("Anterior") }
                Button(onClick = { if (blockIndex == routine.blocks.lastIndex) { running = false; finished = true } else blockIndex += 1 }, modifier = Modifier.weight(1f)) { Text("Siguiente") }
            }
            Button(onClick = { running = false; saveAndExit(false) }, enabled = elapsedBySkill.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Guardar práctica parcial") }
        }
    }
}

@Composable
private fun SkillPicker(selected: String, onSelect: (String) -> Unit) {
    val skills = listOf("general" to "General", "arpegios" to "Arpegios", "cambios-acordes" to "Cambios", "patrones" to "Patrones", "velocidad" to "Velocidad", "tecnica" to "Técnica", "repertorio" to "Repertorio")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skills.forEach { (id, label) -> FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) }) }
    }
}

@Composable
private fun PracticeTimer(elapsedSeconds: Int, remainingSeconds: Int? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(formatDuration(remainingSeconds ?: elapsedSeconds), style = MaterialTheme.typography.displayMedium)
        Text(if (remainingSeconds == null) "Tiempo practicado" else "Tiempo restante")
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
    val byRemainder = secondsBySkill.keys.sortedByDescending { skill -> (secondsBySkill.getValue(skill) * totalMinutes) % totalSeconds }
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
