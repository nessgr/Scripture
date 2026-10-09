package app.scripture.domain

import kotlinx.serialization.Serializable

/**
 * How well a verse is known — the only state the scheduler evolves.
 * Defaults mean "brand new": never reviewed, due immediately.
 */
@Serializable
data class ReviewProgress(
    val repetitions: Int = 0,       // consecutive successful reviews
    val easiness: Double = 2.5,    // SM-2 "easiness factor", clamped 1.3..3.0
    val intervalDays: Int = 0,     // days until the next review after the last one
    val dueEpochDay: Long? = null,  // null = never reviewed, due right away
)

/** Self-grade buttons after revealing a verse. */
enum class Grade { AGAIN, HARD, GOOD, EASY }

/** Which activity produced a review. */
enum class ReviewMode { FLASHCARD, FILL_IN_BLANK, FIRST_LETTERS, REVERSE, TYPE_WHOLE }