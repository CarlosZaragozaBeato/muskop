package com.muskop.android.session

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.muskop.session.SessionDocument
import com.muskop.session.SessionFactory
import com.muskop.session.SessionFileStore
import com.muskop.session.SessionImporter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSessionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val activeSessionFile = File(appContext.filesDir, "sessions/active-session.muskop.json")

    suspend fun loadActive(): SessionDocument? = withContext(Dispatchers.IO) {
        if (!activeSessionFile.exists()) return@withContext null
        SessionFileStore.read(activeSessionFile.toPath())
    }

    suspend fun create(username: String): SessionDocument = withContext(Dispatchers.IO) {
        val session = SessionFactory.create(username)
        save(session)
        session
    }

    suspend fun importFrom(uri: Uri): SessionDocument = withContext(Dispatchers.IO) {
        val text = appContext.contentResolver.readText(uri)
        val session = SessionImporter.fromJson(text)
        save(session)
        session
    }

    suspend fun save(session: SessionDocument) = withContext(Dispatchers.IO) {
        SessionFileStore.writeAtomically(activeSessionFile.toPath(), session)
    }

    suspend fun exportTo(uri: Uri, session: SessionDocument) = withContext(Dispatchers.IO) {
        appContext.contentResolver.openOutputStream(uri, "wt")
            ?.use { stream -> stream.write(session.toJson().toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("No se pudo abrir el destino de exportación")
    }

    private fun ContentResolver.readText(uri: Uri): String = openInputStream(uri)
        ?.use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        ?: throw IllegalArgumentException("No se pudo abrir el archivo seleccionado")
}
