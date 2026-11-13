package com.muskop.android.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.muskop.session.MAX_RESOURCE_IMPORT_BYTES
import com.muskop.session.MAX_RESOURCE_IMPORT_FILE_BYTES
import com.muskop.session.MAX_RESOURCE_IMPORT_ITEMS
import com.muskop.session.ResourceImportBatch
import com.muskop.session.ResourceImportSource
import com.muskop.session.ResourceStatus
import com.muskop.session.SessionResource
import com.muskop.session.organizationStatus
import com.muskop.session.prepareResourceImport
import com.muskop.session.prepareResourceImports
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ResourceImportDialog(
    existingResources: List<SessionResource>,
    onImport: (ResourceImportBatch, (Result<Int>) -> Unit) -> Unit,
    onImported: (Int, Int) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf<ResourceImportBatch?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val busy = loading || saving
    val previews = remember(batch) { batch?.resources.orEmpty() }
    val titles = remember(existingResources) { existingResources.map { it.title.lowercase(Locale.ROOT) }.toSet() }
    val duplicates = previews.count { it.title.lowercase(Locale.ROOT) in titles }
    val inactive = previews.count { it.organizationStatus() != ResourceStatus.ACTIVE }

    fun prepare(load: suspend () -> ResourceImportBatch) {
        batch = null; error = null; loading = true
        scope.launch {
            try { batch = withContext(Dispatchers.IO) { load() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Could not read these resources." }
            finally { loading = false }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) prepare {
            require(uris.size <= MAX_RESOURCE_IMPORT_ITEMS) { "Choose at most $MAX_RESOURCE_IMPORT_ITEMS files." }
            var total = 0L
            val sources = uris.mapIndexed { index, uri ->
                val name = context.resourceImportName(uri) ?: "File ${index + 1}"
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= MAX_RESOURCE_IMPORT_FILE_BYTES) {
                        val count = input.read(buffer, 0, minOf(buffer.size, MAX_RESOURCE_IMPORT_FILE_BYTES + 1 - output.size()))
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: throw IllegalArgumentException("$name: cannot open this file.")
                require(bytes.size <= MAX_RESOURCE_IMPORT_FILE_BYTES) { "$name: file is larger than 2 MB." }
                total += bytes.size
                require(total <= MAX_RESOURCE_IMPORT_BYTES) { "The selected files exceed 10 MB in total." }
                ResourceImportSource(name, bytes.toString(Charsets.UTF_8))
            }
            prepareResourceImports(sources)
        }
    }
    Dialog(onDismissRequest = { if (!saving) onClose() }) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Import resources", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose, enabled = !saving) { Icon(Icons.Rounded.Close, contentDescription = "Close import") }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Choose multiple JSON files or paste a resource package. TAB and THEORY resources are added as new copies to this session.")
                        TextButton(enabled = !busy, onClick = { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                            Text("Choose files")
                        }
                        OutlinedTextField(text, { text = it; batch = null; error = null }, Modifier.fillMaxWidth(),
                            label = { Text("Resource JSON or array") }, minLines = 3, maxLines = 7, enabled = !busy)
                        TextButton(enabled = text.isNotBlank() && !busy, onClick = { val raw = text; prepare { prepareResourceImport(raw) } }) {
                            Text("Review pasted JSON")
                        }
                        Text("Up to 100 resources · 2 MB per file · 10 MB total", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (busy) item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator()
                            Text(if (saving) "Saving resources…" else "Checking resources…")
                        }
                    }
                    error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                    if (batch != null) item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${previews.size} resources ready to import", style = MaterialTheme.typography.titleMedium)
                            Text("Existing resources and practice history are kept.")
                            if (duplicates > 0) Text("$duplicates titles already exist. They will be added as separate resources.")
                            if (inactive > 0) Text("$inactive resources are outside Active. Use All resources after importing to find them.")
                        }
                    }
                    items(previews, key = { it.id }) { resource ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(resource.title, style = MaterialTheme.typography.titleMedium)
                            Text(resource.type, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            resource.organizationLabel()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                Button(enabled = batch != null && !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    val prepared = batch ?: return@Button
                    if (!saving) {
                        saving = true; error = null
                        onImport(prepared) { result ->
                            saving = false
                            result.fold(onSuccess = { count -> onImported(count, inactive) },
                                onFailure = { failure -> error = failure.message ?: "No resources could be saved. Try again." })
                        }
                    }
                }) { Text("Import ${previews.size} resources") }
            }
        }
    }
}

private fun Context.resourceImportName(uri: Uri): String? = contentResolver
    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && cursor.moveToFirst()) cursor.getString(column)?.take(100) else null
    }
