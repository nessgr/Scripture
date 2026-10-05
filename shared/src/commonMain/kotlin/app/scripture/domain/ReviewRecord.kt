package app.scripture.domain

import kotlinx.serialization.Serializable

/**
 * One completed review, appended to history. Never edited —
 * the log is the source of truth for stats later (streaks, counts).
 */
@Serializable
data class ReviewRecord(
    val verseId: String,
    val epochDay: Long,
    val grade: Grade,
    val mode: ReviewMode,
)