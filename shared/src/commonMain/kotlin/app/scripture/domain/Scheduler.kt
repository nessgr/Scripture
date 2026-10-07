package app.scripture.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Simplified SM-2 spaced repetition.
 *
 * A *pure function*: (progress, grade, today) → new progress.
 * No clocks, no I/O — which is exactly why it is easy to reason
 * about and unit-test. Semantics:
 *
 *  AGAIN: repetitions reset; due again today (the session re-queues it).
 *  HARD : short growth (×1.2), easiness drops slightly.
 *  GOOD : the standard ladder — 1d, 6d, then interval × easiness.
 *  EASY : a faster ladder with an easiness boost.
 */
fun nextProgress(current: ReviewProgress, grade: Grade, todayEpochDay: Long): ReviewProgress {
    val repetitions = current.repetitions
    val interval = current.intervalDays
    val easiness = current.easiness

    return when (grade) {
        Grade.AGAIN -> current.copy(
            repetitions = 0,
            intervalDays = 0,
            easiness = max(1.3, easiness - 0.20),
            dueEpochDay = todayEpochDay,
        )

        Grade.HARD -> {
            val next = if (repetitions == 0) 1 else max(1, (interval * 1.2).roundToInt())
            current.copy(
                repetitions = repetitions + 1,
                intervalDays = next,
                easiness = max(1.3, easiness - 0.15),
                dueEpochDay = todayEpochDay + next,
            )
        }

        Grade.GOOD -> {
            // The classic SM-2 ladder for first two successes, then multiply.
            val next = when (repetitions) {
                0 -> 1
                1 -> 6
                else -> (interval * easiness).roundToInt()
            }
            current.copy(
                repetitions = repetitions + 1,
                intervalDays = next,
                dueEpochDay = todayEpochDay + next,
            )
        }

        Grade.EASY -> {
            val next = when (repetitions) {
                0 -> 3
                1 -> 10
                else -> (interval * easiness * 1.3).roundToInt()
            }
            current.copy(
                repetitions = repetitions + 1,
                intervalDays = next,
                easiness = min(3.0, easiness + 0.15),
                dueEpochDay = todayEpochDay + next,
            )
        }
    }
}

/** A verse is due if it has never been reviewed or its due date has arrived. */
fun isDue(progress: ReviewProgress, todayEpochDay: Long): Boolean =
    (progress.dueEpochDay ?: todayEpochDay) <= todayEpochDay

/**
 * Today's review queue: due verses first (oldest due date first),
 * capped so a session stays focused. A verse never reviewed counts as
 * due *today*, so it comes after anything already overdue.
 */
fun buildReviewQueue(verses: List<Verse>, todayEpochDay: Long, limit: Int = 20): List<Verse> =
    verses
        .filter { isDue(it.progress, todayEpochDay) }
        .sortedWith(
            compareBy({ it.progress.dueEpochDay ?: todayEpochDay }, { it.createdEpochDay }),
        )
        .take(limit)