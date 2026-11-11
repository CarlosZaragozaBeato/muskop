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
            throw SessionFormatException("The file does not contain a valid JSON session", error)
        }
        return fromObject(root)
    }

    fun fromObject(root: JsonObject): SessionDocument {
        val version = root["muskopSession"]?.jsonPrimitive?.intOrNull
            ?: throw SessionFormatException("muskopSession is missing or is not an integer")

        validateEnvelope(root)

        val migrated = when (version) {
            1 -> migrateV1(root)
            CURRENT_SESSION_VERSION -> root
            in 0 until CURRENT_SESSION_VERSION -> throw SessionFormatException(
                "Session version $version has no available migrator",
            )
            else -> throw SessionFormatException(
                "The session uses version $version, which is newer than the app ($CURRENT_SESSION_VERSION)",
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
            ?: throw SessionFormatException("The session is missing the user object")
        val username = user["username"]?.jsonPrimitive?.contentOrNull
        if (username.isNullOrBlank()) {
            throw SessionFormatException("The session is missing user.username")
        }

        for (key in REQUIRED_COLLECTIONS) {
            if (root[key] !is kotlinx.serialization.json.JsonArray) {
                throw SessionFormatException("The $key field must be a JSON array")
            }
        }
    }

    private val REQUIRED_COLLECTIONS = setOf("resources", "routines", "practiceLog")
}

