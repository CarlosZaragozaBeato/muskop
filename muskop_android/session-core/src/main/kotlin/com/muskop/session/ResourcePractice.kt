package com.muskop.session

import java.time.Instant

/** Time that actually elapsed. A resource may be absent without losing total/skill time. */
data class PracticeTime(
    val skillSeconds: Map<String, Int> = emptyMap(),
    val resourceSeconds: Map<String, Int> = emptyMap(),
    val resourceLastPracticedAt: Map<String, String> = emptyMap(),
) : java.io.Serializable {
    val totalSeconds: Int get() = skillSeconds.values.sum()

    fun tick(skill: String, resourceId: String?, at: String = Instant.now().toString()): PracticeTime {
        val skills = skillSeconds + (skill to ((skillSeconds[skill] ?: 0) + 1))
        if (resourceId.isNullOrBlank()) return copy(skillSeconds = skills)
        return copy(
            skillSeconds = skills,
            resourceSeconds = resourceSeconds + (resourceId to ((resourceSeconds[resourceId] ?: 0) + 1)),
            resourceLastPracticedAt = resourceLastPracticedAt + (resourceId to at),
        )
    }
}

data class ResourcePracticeSummary(val totalSeconds: Long, val lastPracticedAt: String?, val practiceCount: Int)

fun SessionDocument.resourcePracticeSummary(resourceId: String): ResourcePracticeSummary {
    val entries = practiceEntries().withIndex().distinctBy { (index, entry) ->
        (entry.id != null) to (entry.id ?: index.toString())
    }.map { it.value }.filter { (it.resourceSeconds[resourceId] ?: 0) > 0 }
    val last = entries.mapNotNull { entry ->
        entry.resourceLastPracticedAt[resourceId]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }.maxOrNull()?.toString()
    return ResourcePracticeSummary(entries.sumOf { it.resourceSeconds.getValue(resourceId).toLong() }, last, entries.size)
}

internal fun validateResourcePractice(seconds: Map<String, Int>, lastAt: Map<String, String>, totalSeconds: Int) {
    require(seconds.keys.all { it.isNotBlank() } && seconds.values.all { it >= 0 }) { "Resource seconds must be non-negative with non-empty ids" }
    require(seconds.values.sumOf { it.toLong() } <= totalSeconds.toLong()) { "Resource time cannot exceed total practice time" }
    require(lastAt.keys == seconds.filterValues { it > 0 }.keys) { "Each practiced resource needs its last practice time" }
    require(lastAt.values.all { runCatching { Instant.parse(it) }.isSuccess }) { "Resource practice times must be ISO instants" }
}
