package com.muskop.android.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muskop.session.ResourceEditorInput
import com.muskop.session.SessionResource
import com.muskop.session.theoryContent
import com.muskop.session.TabSectionInput
import com.muskop.session.ensureTabSectionIds
import com.muskop.session.importTabResource
import com.muskop.session.tabSections
import com.muskop.session.updateTabSections
import com.muskop.session.updateTabSectionMeasures
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ResourceEditorScreen(
    resource: SessionResource?,
    padding: PaddingValues,
    onSave: (ResourceEditorInput) -> Unit,
    onBack: () -> Unit,
) {
    var type by remember(resource) { mutableStateOf(resource?.type ?: "THEORY") }
    var title by remember(resource) { mutableStateOf(resource?.title.orEmpty()) }
    var category by remember(resource) { mutableStateOf(resource?.category.orEmpty()) }
    var tags by remember(resource) { mutableStateOf(resource?.tags?.joinToString(", ").orEmpty()) }
    var body by remember(resource) { mutableStateOf(theoryBody(resource)) }
    var tabContent by remember(resource) { mutableStateOf(ensureTabSectionIds(resource?.content ?: JsonObject(mapOf(
        "version" to JsonPrimitive(2),
        "tuning" to JsonArray(listOf("E", "A", "D", "G", "B", "E").map(::JsonPrimitive)),
        "timeSignature" to JsonPrimitive("4/4"),
        "baseBpm" to JsonPrimitive(80),
        "sections" to JsonArray(emptyList()),
    )))) }
    var sections by remember(resource) {
        mutableStateOf(resource?.copy(content = tabContent)?.tabSections()?.map { TabSectionInput(it.id, it.kind, it.name, it.bpm, it.notes) }.orEmpty())
    }
    var showManualEditor by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    fun applyImport(raw: String) {
        try {
            val input = importTabResource(raw)
            val imported = SessionResource("", input.title, input.type, input.category, input.tags, input.content, null)
            val importedSections = imported.tabSections().map { TabSectionInput(it.id, it.kind, it.name, it.bpm, it.notes) }
            tabContent = input.content
            sections = importedSections
            title = imported.title
            category = input.category.orEmpty()
            tags = input.tags.joinToString(", ")
            type = "TAB"
            importError = null
            showImport = false
        } catch (error: IllegalArgumentException) { importError = error.message ?: "Could not import tablature." }
        catch (_: IllegalStateException) { importError = "The tablature contains invalid section fields." }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= 2_000_000) {
                        val count = input.read(buffer, 0, minOf(buffer.size, 2_000_001 - output.size()))
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                    ?: throw java.io.IOException("Cannot open this file.")
                if (bytes.size > 2_000_000) throw java.io.IOException("The file is larger than 2 MB.")
                applyImport(bytes.toString(Charsets.UTF_8))
            } catch (error: java.io.IOException) {
                importError = error.message ?: "Could not read this file."
            }
        }
    }
    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Import tablature") },
            text = { Column {
                Text("Paste generated JSON or choose a JSON file. Save the resource to add it to your library.")
                TextButton(onClick = { filePicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Choose file") }
                OutlinedTextField(importText, { importText = it; importError = null }, Modifier.fillMaxWidth(), label = { Text("Tablature JSON") }, minLines = 6, maxLines = 12, isError = importError != null)
                importError?.let { Text(it) }
            } },
            confirmButton = { TextButton(onClick = { applyImport(importText) }) { Text("Use tablature") } },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("Cancel") } },
        )
    }
    val theory = type.equals("THEORY", ignoreCase = true)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("← Back to library") } }
        item { Text(if (resource == null) "New resource" else "Edit resource") }
        if (resource == null) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == "THEORY", onClick = { type = "THEORY" }, label = { Text("Theory") })
                FilterChip(selected = type == "TAB", onClick = { type = "TAB" }, label = { Text("Tablature") })
            }
        }
        if (resource == null) item { TextButton(onClick = { showImport = true }) { Text("Import generated tablature") } }
        item { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true) }
        item { OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true) }
        item { OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = { Text("Comma-separated tags") }, singleLine = true) }
        if (theory) item {
            OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Content") }, minLines = 10)
        }
        if (!theory) {
            item {
                Text("Tablature preview")
                ResourcePreview(SessionResource("preview", title, "TAB", category, emptyList(), tabContent, null))
            }
            item {
                TextButton(onClick = { showManualEditor = !showManualEditor }) {
                    Text(if (showManualEditor) "Hide manual editor" else "Open manual editor")
                }
            }
            if (showManualEditor) item {
                val document = tabContent as? JsonObject
                val signature = (document?.get("timeSignature") as? JsonPrimitive)?.contentOrNull
                val tuning = document?.get("tuning") as? JsonArray
                if ((signature != null && signature != "4/4") || (tuning != null && tuning.size != 6)) {
                    Text("Manual editing is available for six-string 4/4 tablatures. This document can still be saved and viewed.")
                } else {
                    TabSectionEditor(
                        sections = sections,
                        content = tabContent,
                        onChange = { sections = it; tabContent = updateTabSections(tabContent, it) },
                        onMeasureChange = { sectionId, index, measure ->
                            tabContent = updateTabSectionMeasures(tabContent, sectionId, index, measure)
                        },
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    onSave(
                        ResourceEditorInput(
                            id = resource?.id,
                            title = title,
                            type = type,
                            category = category,
                            tags = tags.split(',').map(String::trim).filter(String::isNotEmpty),
                            content = if (theory) theoryContent(body, resource?.content) else tabContent,
                        ),
                    )
                    onBack()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save resource") }
        }
    }
}

private fun theoryBody(resource: SessionResource?): String = ((resource?.content) as? JsonObject)
    ?.get("body")
    ?.jsonPrimitive
    ?.contentOrNull
    .orEmpty()
