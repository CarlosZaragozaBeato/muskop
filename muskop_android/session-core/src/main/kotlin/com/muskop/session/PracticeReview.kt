package com.muskop.session

data class PracticedResourceReview(
    val id: String,
    val resource: SessionResource?,
    val seconds: Int,
    val history: ResourcePracticeSummary,
)

data class PracticeDaySummary(
    val date: String,
    val seconds: Long,
    val practiceCount: Int,
    val resourceCount: Int,
    val completedRoutineCount: Int,
)

data class PracticeReview(
    val entry: PracticeLogEntry,
    val resources: List<PracticedResourceReview>,
    val unassignedSeconds: Long,
    val day: PracticeDaySummary,
)

/** A projection of saved history and current assessments; never predicts learning from time. */
fun SessionDocument.practiceReview(practiceId: String): PracticeReview? {
    val entries = practiceEntries().withIndex().distinctBy { (index, entry) ->
        (entry.id != null) to (entry.id ?: index.toString())
    }.map { it.value }
    val entry = entries.firstOrNull { it.id == practiceId } ?: return null
    // Ambiguous or removed identities keep their history, but cannot offer an assessment editor.
    val library = resources().groupBy { it.id }.mapValues { it.value.singleOrNull() }
    val practiced = entry.resourceSeconds.filterValues { it > 0 }.map { (id, seconds) ->
        PracticedResourceReview(id, library[id], seconds, resourcePracticeSummary(id))
    }
    val day = entries.filter { it.date == entry.date && it.seconds > 0 }
    return PracticeReview(
        entry, practiced,
        (entry.seconds.toLong() - practiced.sumOf { it.seconds.toLong() }).coerceAtLeast(0),
        PracticeDaySummary(
            entry.date, day.sumOf { it.seconds.toLong() }, day.size,
            day.flatMap { it.resourceSeconds.filterValues { seconds -> seconds > 0 }.keys }.toSet().size,
            day.count { it.routineId != "free" && it.completed },
        ),
    )
}
