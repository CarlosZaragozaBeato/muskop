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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.muskop.android.R
import com.muskop.android.library.LibraryScreen
import com.muskop.android.practice.RoutinesScreen
import com.muskop.android.progress.ProgressScreen
import com.muskop.android.session.SessionUiState
import com.muskop.session.ActivePracticeSnapshot
import com.muskop.session.PracticeEntryInput
import com.muskop.session.ResourceEditorInput
import com.muskop.session.RoutineEditorInput
import com.muskop.session.SessionDocument

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Rounded.Home),
    Routines("Rutinas", Icons.Rounded.QueueMusic),
    Library("Biblioteca", Icons.Rounded.LibraryMusic),
    Progress("Progreso", Icons.Rounded.BarChart),
    Settings("Ajustes", Icons.Rounded.Settings),
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
    onSaveActivePractice: (ActivePracticeSnapshot) -> Unit,
    onClearActivePractice: () -> Unit,
    onSaveRoutine: (RoutineEditorInput) -> Unit,
    onSaveResource: (ResourceEditorInput) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var pendingPractice by rememberSaveable { mutableStateOf<String?>(null) }

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
            if (state is SessionUiState.Ready) {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
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
                pendingPractice = pendingPractice,
                onStartPractice = { target ->
                    pendingPractice = target
                    destination = Destination.Routines
                },
                onPracticeConsumed = { pendingPractice = null },
                onImportSession = onImportSession,
                onExportSession = onExportSession,
                onRecordPractice = onRecordPractice,
                onSaveActivePractice = onSaveActivePractice,
                onClearActivePractice = onClearActivePractice,
                onSaveRoutine = onSaveRoutine,
                onSaveResource = onSaveResource,
            )
            is SessionUiState.Error -> ErrorContent(padding, state, onImportSession, onRetry)
        }
    }
}

@Composable
private fun DestinationContent(
    padding: PaddingValues,
    destination: Destination,
    session: SessionDocument,
    pendingPractice: String?,
    onStartPractice: (String) -> Unit,
    onPracticeConsumed: () -> Unit,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
    onRecordPractice: (PracticeEntryInput) -> Unit,
    onSaveActivePractice: (ActivePracticeSnapshot) -> Unit,
    onClearActivePractice: () -> Unit,
    onSaveRoutine: (RoutineEditorInput) -> Unit,
    onSaveResource: (ResourceEditorInput) -> Unit,
) {
    when (destination) {
        Destination.Home -> HomeScreen(session, padding, onStartPractice)
        Destination.Routines -> RoutinesScreen(
            session = session,
            padding = padding,
            initialTarget = pendingPractice,
            onInitialTargetConsumed = onPracticeConsumed,
            onRecordPractice = onRecordPractice,
            onSaveActive = onSaveActivePractice,
            onClearActive = onClearActivePractice,
            onSaveRoutine = onSaveRoutine,
        )
        Destination.Library -> LibraryScreen(session, padding, onSaveResource)
        Destination.Progress -> ProgressScreen(session, padding)
        Destination.Settings -> SettingsScreen(session, padding, onImportSession, onExportSession)
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator() }
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
        Button(onClick = onImportSession) { Text("Importar sesión") }
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
