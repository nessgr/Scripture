package app.scripture

import app.scripture.domain.Grade
import app.scripture.domain.ReviewProgress
import app.scripture.domain.Verse
import app.scripture.domain.buildReviewQueue
import app.scripture.domain.isDue
import app.scripture.domain.nextProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchedulerTest {

    private val today = 20_000L

    @Test
    fun goodFollowsTheClassicLadder() {
        val fresh = ReviewProgress()

        val after1 = nextProgress(fresh, Grade.GOOD, today)
        assertEquals(1, after1.intervalDays)
        assertEquals(today + 1, after1.dueEpochDay)
        assertEquals(1, after1.repetitions)

        val after2 = nextProgress(after1, Grade.GOOD, today + 1)
        assertEquals(6, after2.intervalDays)

        val after3 = nextProgress(after2, Grade.GOOD, today + 7)
        // 6 days × easiness 2.5 = 15
        assertEquals(15, after3.intervalDays)
        assertEquals(3, after3.repetitions)
    }

    @Test
    fun easyUsesAFasterLadder() {
        val after1 = nextProgress(ReviewProgress(), Grade.EASY, today)
        assertEquals(3, after1.intervalDays)

        val after2 = nextProgress(after1, Grade.EASY, today + 3)
        assertEquals(10, after2.intervalDays)
        // Ease is adjusted on every grading (SM-2 semantics): 2.5 + 0.15 + 0.15.
        assertEquals(2.8, after2.easiness, absoluteTolerance = 1e-9)
    }

    @Test
    fun easinessNeverExceedsThree() {
        var progress = ReviewProgress()
        repeat(10) { progress = nextProgress(progress, Grade.EASY, today) }
        assertEquals(3.0, progress.easiness, absoluteTolerance = 1e-9)
    }

    @Test
    fun againResetsTheLadderAndStaysDueToday() {
        val learned = ReviewProgress(repetitions = 4, intervalDays = 40, dueEpochDay = today + 40)

        val after = nextProgress(learned, Grade.AGAIN, today)
        assertEquals(0, after.repetitions)
        assertEquals(0, after.intervalDays)
        assertEquals(today, after.dueEpochDay)
    }

    @Test
    fun easinessNeverDropsBelowOnePointThree() {
        var progress = ReviewProgress(easiness = 1.35)
        repeat(5) { progress = nextProgress(progress, Grade.AGAIN, today) }
        assertEquals(1.3, progress.easiness, absoluteTolerance = 1e-9)
    }

    @Test
    fun hardGrowsSlowly() {
        val learned = ReviewProgress(repetitions = 3, intervalDays = 10)
        val after = nextProgress(learned, Grade.HARD, today)
        assertEquals(12, after.intervalDays) // 10 × 1.2
        assertEquals(today + 12, after.dueEpochDay)
    }

    @Test
    fun freshVersesAreDueAndFutureOnesAreNot() {
        assertTrue(isDue(ReviewProgress(), today))                      // never reviewed
        assertTrue(isDue(ReviewProgress(dueEpochDay = today - 1), today))
        assertTrue(isDue(ReviewProgress(dueEpochDay = today), today))
        assertFalse(isDue(ReviewProgress(dueEpochDay = today + 1), today))
    }

    @Test
    fun queueTakesDueVersesOldestFirstAndCapsTheSession() {
        val dueOld = verse("old", dueEpochDay = today - 5)
        val dueNew = verse("new", dueEpochDay = today - 1)
        val fresh = verse("fresh")                    // dueEpochDay null → due now
        val notDue = verse("future", dueEpochDay = today + 3)

        val queue = buildReviewQueue(listOf(notDue, dueNew, fresh, dueOld), today)

        assertEquals(listOf("old", "new", "fresh"), queue.map { it.id })
    }

    @Test
    fun queueRespectsTheLimit() {
        val verses = (1..30).map { i ->
            verse("v$i", dueEpochDay = today - i) // all due, oldest = v30
        }
        val queue = buildReviewQueue(verses, today, limit = 20)
        assertEquals(20, queue.size)
        assertEquals("v30", queue.first().id) // oldest due date first
    }

    private fun verse(id: String, dueEpochDay: Long? = null) = Verse(
        id = id,
        reference = "Test $id",
        text = "Text of $id",
        createdEpochDay = 19_000,
        progress = ReviewProgress(dueEpochDay = dueEpochDay),
    )
}