package com.muskop.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muskop.android.session.SessionViewModel
import com.muskop.android.ui.MuskopApp
import com.muskop.android.ui.MuskopTheme

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by sessionViewModel.state.collectAsStateWithLifecycle()
            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri -> uri?.let(sessionViewModel::importSession) }
            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri -> uri?.let(sessionViewModel::exportSession) }

            MuskopTheme {
                MuskopApp(
                    state = state,
                    onCreateSession = sessionViewModel::create,
                    onImportSession = {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    onExportSession = { exportLauncher.launch("Muskop.muskop.json") },
                    onRetry = sessionViewModel::refresh,
                    onRecordPractice = sessionViewModel::recordPractice,
                    onReviewResourceLearning = sessionViewModel::reviewResourceLearning,
                    onSaveActivePractice = sessionViewModel::saveActivePractice,
                    onClearActivePractice = sessionViewModel::clearActivePractice,
                    onSaveRoutine = sessionViewModel::saveRoutine,
                    onDeleteRoutine = sessionViewModel::deleteRoutine,
                    onSavePracticeGoals = sessionViewModel::savePracticeGoals,
                    onSaveResource = sessionViewModel::saveResource,
                    onImportResources = sessionViewModel::importResources,
                    onDeleteResource = sessionViewModel::deleteResource,
                    onUpdateResourceLearning = sessionViewModel::updateResourceLearning,
                    onUpdateResourceOrganization = sessionViewModel::updateResourceOrganization,
                )
            }
        }
    }
}
