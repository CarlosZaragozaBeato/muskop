package com.muskop.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.session.SessionDocument

@Composable
fun SettingsScreen(
    session: SessionDocument,
    padding: PaddingValues,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
) {
    val inventory = session.inventory()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(session.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Sesión Muskop v${session.version}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("${inventory.resources} recursos · ${inventory.routines} rutinas · ${inventory.practiceEntries} prácticas")
                    }
                }
            }
        }
        item { Text("Archivo de sesión", style = MaterialTheme.typography.titleLarge) }
        item {
            FilledTonalButton(onClick = onExportSession, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Text("Exportar copia de seguridad", Modifier.padding(start = 10.dp))
            }
        }
        item {
            FilledTonalButton(onClick = onImportSession, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null)
                Text("Importar otra sesión", Modifier.padding(start = 10.dp))
            }
        }
    }
}
