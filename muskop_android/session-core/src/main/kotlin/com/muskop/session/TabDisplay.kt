package com.muskop.session

/** Length in quarter-note beats, matching the document's beat coordinate. */
fun tabMeasureLength(signature: String?): Double? {
    if (signature == null) return 4.0
    val parts = signature.split('/')
    if (parts.size != 2) return null
    val numerator = parts[0].toIntOrNull() ?: return null
    val denominator = parts[1].toIntOrNull() ?: return null
    if (numerator !in 1..32 || denominator !in listOf(1, 2, 4, 8, 16, 32)) return null
    return numerator * 4.0 / denominator
}

/** Use the declared tuning and visible notes, with a bounded size for the diagram. */
fun tabDisplayStringCount(tuningCount: Int, noteStrings: Iterable<Int>): Int {
    val maximumNote = noteStrings.filter { it in 1..24 }.maxOrNull() ?: 0
    return maxOf(tuningCount.takeIf { it in 1..24 } ?: 0, maximumNote, if (tuningCount == 0) 6 else 1)
}
