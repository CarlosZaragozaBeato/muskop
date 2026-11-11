package com.muskop.android.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import com.muskop.session.tabSections
import com.muskop.session.updateTabSections
import com.muskop.session.updateTabSectionMeasures
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ResourceEditorScreen(
    resource: SessionResource?,
    padding: PaddingValues,
    onSave: (ResourceEditorInput) -> Unit,
    onBack: () -> Unit,
) {
    val type = resource?.type ?: "THEORY"
    var title by remember(resource) { mutableStateOf(resource?.title.orEmpty()) }
    var category by remember(resource) { mutableStateOf(resource?.category.orEmpty()) }
    var tags by remember(resource) { mutableStateOf(resource?.tags?.joinToString(", ").orEmpty()) }
    var body by remember(resource) { mutableStateOf(theoryBody(resource)) }
    var tabContent by remember(resource) { mutableStateOf(resource?.content ?: JsonObject(emptyMap())) }
    var sections by remember(resource) {
        mutableStateOf(resource?.tabSections()?.map { TabSectionInput(it.id, it.kind, it.name, it.bpm, it.notes) }.orEmpty())
    }
    val theory = type.equals("THEORY", ignoreCase = true)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("← Volver a biblioteca") } }
        item { Text(if (resource == null) "Nueva nota" else "Editar recurso") }
        item { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Título") }, singleLine = true) }
        item { OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Categoría") }, singleLine = true) }
        item { OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = { Text("Etiquetas separadas por comas") }, singleLine = true) }
        if (theory) item {
            OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Contenido") }, minLines = 10)
        }
        if (!theory) item {
            TabSectionEditor(
                sections = sections,
                content = tabContent,
                onChange = { sections = it },
                onMeasureChange = { sectionId, index, measure ->
                    tabContent = updateTabSectionMeasures(tabContent, sectionId, index, measure)
                },
            )
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
                            content = if (theory) theoryContent(body) else updateTabSections(tabContent, sections),
                        ),
                    )
                    onBack()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar recurso") }
        }
    }
}

private fun theoryBody(resource: SessionResource?): String = ((resource?.content) as? JsonObject)
    ?.get("body")
    ?.jsonPrimitive
    ?.contentOrNull
    .orEmpty()
