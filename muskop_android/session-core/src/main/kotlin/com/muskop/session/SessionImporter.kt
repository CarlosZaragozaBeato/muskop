package com.muskop.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val parser = Json {
    ignoreUnknownKeys = false
    explicitNulls = true
}

class SessionFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object SessionImporter {
    fun fromJson(text: String): SessionDocument {
        val root = try {
            parser.parseToJsonElement(text).jsonObject
        } catch (error: Exception) {
            throw SessionFormatException("El archivo no contiene una sesión JSON válida", error)
        }
        return fromObject(root)
    }

    fun fromObject(root: JsonObject): SessionDocument {
        val version = root["muskopSession"]?.jsonPrimitive?.intOrNull
            ?: throw SessionFormatException("Falta muskopSession o no es un número entero")

        validateEnvelope(root)

        val migrated = when (version) {
            1 -> migrateV1(root)
            CURRENT_SESSION_VERSION -> root
            in 0 until CURRENT_SESSION_VERSION -> throw SessionFormatException(
                "La versión de sesión $version no tiene migrador disponible",
            )
            else -> throw SessionFormatException(
                "La sesión usa la versión $version, más reciente que la app ($CURRENT_SESSION_VERSION)",
            )
        }

        return SessionDocument(migrated)
    }

    private fun migrateV1(root: JsonObject): JsonObject = JsonObject(
        root.toMutableMap().apply {
            this["muskopSession"] = JsonPrimitive(CURRENT_SESSION_VERSION)
        },
    )

    private fun validateEnvelope(root: JsonObject) {
        val user = root["user"] as? JsonObject
            ?: throw SessionFormatException("Falta el objeto user en la sesión")
        val username = user["username"]?.jsonPrimitive?.contentOrNull
        if (username.isNullOrBlank()) {
            throw SessionFormatException("Falta user.username en la sesión")
        }

        for (key in REQUIRED_COLLECTIONS) {
            if (root[key] !is kotlinx.serialization.json.JsonArray) {
                throw SessionFormatException("El campo $key debe ser una lista JSON")
            }
        }
    }

    private val REQUIRED_COLLECTIONS = setOf("resources", "routines", "practiceLog")
}

