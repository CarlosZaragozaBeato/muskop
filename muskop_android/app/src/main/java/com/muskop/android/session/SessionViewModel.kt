package com.muskop.android.session

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.muskop.session.SessionDocument
import com.muskop.session.PracticeEntryInput
import com.muskop.session.ActivePracticeSnapshot
import com.muskop.session.RoutineEditorInput
import com.muskop.session.ResourceEditorInput
import com.muskop.session.clearActivePractice
import com.muskop.session.recordPractice
import com.muskop.session.saveActivePractice
import com.muskop.session.saveRoutine
import com.muskop.session.saveResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Empty : SessionUiState
    data class Ready(val session: SessionDocument) : SessionUiState
    data class Error(val message: String, val session: SessionDocument? = null) : SessionUiState
}

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AndroidSessionRepository(application)
    private val mutableState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    private val mutationMutex = Mutex()

    val state: StateFlow<SessionUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = SessionUiState.Loading
            mutableState.value = runCatching { repository.loadActive() }
                .fold(
                    onSuccess = { session -> session?.let(SessionUiState::Ready) ?: SessionUiState.Empty },
                    onFailure = { error -> SessionUiState.Error(error.message ?: "The session could not be opened") },
                )
        }
    }

    fun create(username: String) {
        viewModelScope.launch {
            mutableState.value = SessionUiState.Loading
            mutableState.value = runCatching { repository.create(username) }
                .fold(
                    onSuccess = SessionUiState::Ready,
                    onFailure = { error -> SessionUiState.Error(error.message ?: "The session could not be created") },
                )
        }
    }

    fun importSession(uri: Uri) {
        viewModelScope.launch {
            val retained = (mutableState.value as? SessionUiState.Ready)?.session
            mutableState.value = SessionUiState.Loading
            mutableState.value = runCatching { repository.importFrom(uri) }
                .fold(
                    onSuccess = SessionUiState::Ready,
                    onFailure = { error ->
                        SessionUiState.Error(error.message ?: "The session could not be imported", retained)
                    },
                )
        }
    }

    fun exportSession(uri: Uri) {
        val session = (mutableState.value as? SessionUiState.Ready)?.session ?: return
        viewModelScope.launch {
            mutableState.value = runCatching {
                repository.exportTo(uri, session)
                session
            }.fold(
                onSuccess = SessionUiState::Ready,
                onFailure = { error ->
                    SessionUiState.Error(error.message ?: "The session could not be exported", session)
                },
            )
        }
    }

    fun recordPractice(entry: PracticeEntryInput) {
        mutateSession("The practice could not be saved") { session -> session.recordPractice(entry) }
    }

    fun saveActivePractice(snapshot: ActivePracticeSnapshot) {
        mutateSession("The active practice could not be saved") { session -> session.saveActivePractice(snapshot) }
    }

    fun clearActivePractice() {
        mutateSession("The active practice could not be cleared") { session -> session.clearActivePractice() }
    }

    fun saveRoutine(input: RoutineEditorInput) {
        mutateSession("The routine could not be saved") { session -> session.saveRoutine(input) }
    }

    fun saveResource(input: ResourceEditorInput) {
        mutateSession("The resource could not be saved") { session -> session.saveResource(input) }
    }

    private fun mutateSession(errorMessage: String, update: (SessionDocument) -> SessionDocument) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val session = (mutableState.value as? SessionUiState.Ready)?.session ?: return@withLock
                try {
                    val updated = update(session)
                    repository.save(updated)
                    mutableState.value = SessionUiState.Ready(updated)
                } catch (error: Exception) {
                    mutableState.value = SessionUiState.Error(error.message ?: errorMessage, session)
                }
            }
        }
    }
}
