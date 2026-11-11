package com.muskop.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muskop.android.R
import com.muskop.android.library.LibraryScreen
import com.muskop.android.practice.RoutinesScreen
import com.muskop.android.progress.ProgressScreen
import com.muskop.session.PracticeEntryInput
import com.muskop.android.session.SessionUiState
import com.muskop.session.SessionDocument

private enum class Destination(val label: String) {
    Today("Hoy"),
    Library("Biblioteca"),
    Progress("Progreso"),
    Routines("Rutinas"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuskopApp(
    state: SessionUiState,
    onCreateSession: (String) -> Unit,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
    onRetry: () -> Unit,
    onRecordPractice: (PracticeEntryInput) -> Unit,
    onSaveActivePractice: (com.muskop.session.ActivePracticeSnapshot) -> Unit,
    onClearActivePractice: () -> Unit,
    onSaveRoutine: (com.muskop.session.RoutineEditorInput) -> Unit,
    onSaveResource: (com.muskop.session.ResourceEditorInput) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.Today) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(
                            if (isSystemInDarkTheme()) R.drawable.muskop_logo_dark
                            else R.drawable.muskop_logo_light,
                        ),
                        contentDescription = "Muskop",
                        modifier = Modifier.height(32.dp),
                        contentScale = ContentScale.Fit,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Text(item.label.first().toString()) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (state) {
            SessionUiState.Loading -> LoadingContent(padding)
            SessionUiState.Empty -> WelcomeContent(padding, onCreateSession, onImportSession)
            is SessionUiState.Ready -> DestinationContent(
                padding = padding,
                destination = destination,
                session = state.session,
                onImportSession = onImportSession,
                onExportSession = onExportSession,
                onRecordPractice = onRecordPractice,
                onSaveActivePractice = onSaveActivePractice,
                onClearActivePractice = onClearActivePractice,
                onSaveRoutine = onSaveRoutine,
                onSaveResource = onSaveResource,
            )
            is SessionUiState.Error -> ErrorContent(
                padding = padding,
                state = state,
                onImportSession = onImportSession,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun WelcomeContent(
    padding: PaddingValues,
    onCreateSession: (String) -> Unit,
    onImportSession: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tu práctica de guitarra, en tus archivos", style = MaterialTheme.typography.headlineSmall)
        Text("Crea una sesión local o importa una copia existente.")
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nombre de la sesión") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = { onCreateSession(username) }, enabled = username.isNotBlank()) {
            Text("Crear sesión")
        }
        HorizontalDivider()
        Button(onClick = onImportSession, modifier = Modifier.fillMaxWidth()) {
            Text("Importar sesión .muskop.json")
        }
    }
}

@Composable
private fun DestinationContent(
    padding: PaddingValues,
    destination: Destination,
    session: SessionDocument,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
    onRecordPractice: (PracticeEntryInput) -> Unit,
    onSaveActivePractice: (com.muskop.session.ActivePracticeSnapshot) -> Unit,
    onClearActivePractice: () -> Unit,
    onSaveRoutine: (com.muskop.session.RoutineEditorInput) -> Unit,
    onSaveResource: (com.muskop.session.ResourceEditorInput) -> Unit,
) {
    if (destination == Destination.Library) {
        LibraryScreen(session = session, padding = padding, onSaveResource = onSaveResource)
        return
    }
    if (destination == Destination.Routines) {
        RoutinesScreen(
            session = session,
            padding = padding,
            onRecordPractice = onRecordPractice,
            onSaveActive = onSaveActivePractice,
            onClearActive = onClearActivePractice,
            onSaveRoutine = onSaveRoutine,
        )
        return
    }
    if (destination == Destination.Progress) {
        ProgressScreen(session = session, padding = padding)
        return
    }
    if (destination != Destination.Today) {
        PlaceholderContent(padding, destination)
        return
    }

    val inventory = session.inventory()
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Hola, ${session.username}", style = MaterialTheme.typography.headlineMedium)
        Text("La sesión v${session.version} está guardada localmente.")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Resumen de tu sesión", fontWeight = FontWeight.SemiBold)
                Text("${inventory.resources} recursos · ${inventory.routines} rutinas")
                Text("${inventory.practiceEntries} registros de práctica")
                Text("${inventory.projects} proyectos · ${inventory.trashItems} en papelera")
            }
        }
        Button(onClick = onExportSession, modifier = Modifier.fillMaxWidth()) {
            Text("Exportar copia de seguridad")
        }
        Button(onClick = onImportSession, modifier = Modifier.fillMaxWidth()) {
            Text("Importar otra sesión")
        }
        Text("Abre Rutinas para iniciar una rutina o una práctica libre.")
    }
}

@Composable
private fun PlaceholderContent(padding: PaddingValues, destination: Destination) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(destination.label, style = MaterialTheme.typography.headlineMedium)
        Text("Este flujo se implementará sobre la sesión ya preservada.")
    }
}

@Composable
private fun ErrorContent(
    padding: PaddingValues,
    state: SessionUiState.Error,
    onImportSession: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("No se pudo abrir la sesión", style = MaterialTheme.typography.headlineSmall)
        Text(state.message)
        Button(onClick = onRetry) { Text("Reintentar") }
        Button(onClick = onImportSession) { Text("Elegir otro archivo") }
    }
}
