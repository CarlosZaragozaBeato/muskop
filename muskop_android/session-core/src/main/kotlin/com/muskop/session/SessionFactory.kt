package com.muskop.session

import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object SessionFactory {
    fun create(username: String): SessionDocument {
        val normalizedUsername = username.trim()
        require(normalizedUsername.isNotEmpty()) { "Username is required" }

        val now = Instant.now().toString()
        return SessionImporter.fromObject(
            JsonObject(
                mapOf(
                    "muskopSession" to JsonPrimitive(CURRENT_SESSION_VERSION),
                    "user" to JsonObject(
                        mapOf(
                            "username" to JsonPrimitive(normalizedUsername),
                            "label" to kotlinx.serialization.json.JsonNull,
                            "createdAt" to JsonPrimitive(now),
                            "updatedAt" to JsonPrimitive(now),
                        ),
                    ),
                    "resources" to JsonArray(emptyList()),
                    "routines" to JsonArray(emptyList()),
                    "practiceLog" to JsonArray(emptyList()),
                    "experience" to JsonObject(emptyMap()),
                    "bonusXp" to JsonPrimitive(0),
                    "goals" to JsonObject(
                        mapOf(
                            "weekly" to JsonPrimitive(0),
                            "monthly" to JsonPrimitive(0),
                            "yearly" to JsonPrimitive(0),
                        ),
                    ),
                    "goalsClaimed" to JsonArray(emptyList()),
                    "achievements" to JsonArray(emptyList()),
                    "settings" to JsonObject(emptyMap()),
                ),
            ),
        )
    }
}
