package com.muskop.session

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

object SessionFileStore {
    fun read(path: Path): SessionDocument = SessionImporter.fromJson(
        Files.readString(path, StandardCharsets.UTF_8),
    )

    fun writeAtomically(path: Path, session: SessionDocument) {
        val parent = path.toAbsolutePath().parent
            ?: throw IllegalArgumentException("La sesión debe tener un directorio destino")
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, ".${path.fileName}.", ".tmp")

        try {
            Files.writeString(temporary, session.toJson(), StandardCharsets.UTF_8)
            try {
                Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

