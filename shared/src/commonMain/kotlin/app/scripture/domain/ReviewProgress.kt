package app.scripture.domain

import kotlinx.serialization.Serializable

/**
 * Where a verse stands on the mastery ladder (see domain/Drills.kt):
 * each review climbs one rung on GOOD/EASY and drops one on AGAIN, so
 * difficulty converges on full recall and *stays* there instead of
 * looping back to easy drills forever.
 *
 * Stored per-verse because repetitions can't express it — the count only
 * says how often the verse was graded, not how much of it the drills
 * should ask for.
 */
@Serializable
enum class ReviewStage {
    READ,          // flashcard: reference shown, recall the text
    CLOZE_30,      // type the blanked words — 30% of the verse
    CLOZE_60,
    CLOZE_90,
    FIRST_LETTERS, // initials shown, type the whole verse
    REVERSE,       // text shown, recall the reference
    FULL_RECALL,   // reference shown, type the whole verse — the top, forever
}

/**
 * How well a verse is known — the only state the scheduler evolves.
 * Defaults mean "brand new": never reviewed, due immediately.
 *
 * [stage] is nullable for migration: files written before the ladder
 * existed have no field, and `null` marks "not yet placed" — the store
 * derives a rung from `repetitions` the first time it loads such a file.
 */
@Serializable
data class ReviewProgress(
    val repetitions: Int = 0,       // consecutive successful reviews
    val easiness: Double = 2.5,    // SM-2 "easiness factor", clamped 1.3..3.0
    val intervalDays: Int = 0,     // days until the next review after the last one
    val dueEpochDay: Long? = null,  // null = never reviewed, due right away
    val stage: ReviewStage? = null, // null = pre-ladder data, see above
)

/** Self-grade buttons after revealing a verse. */
enum class Grade { AGAIN, HARD, GOOD, EASY }

/** Which activity produced a review. */
enum class ReviewMode { FLASHCARD, FILL_IN_BLANK, FIRST_LETTERS, REVERSE, TYPE_WHOLE }