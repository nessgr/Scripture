package app.scripture

import app.scripture.domain.ReviewMode
import app.scripture.domain.checkTypedVerse
import app.scripture.domain.firstLetters
import app.scripture.domain.maxBlanksFor
import app.scripture.domain.normalizeWord
import app.scripture.domain.reviewModeFor
import kotlin.test.Test
import kotlin.test.assertEquals

class DrillsTest {

    // ---- rotation ----

    @Test
    fun firstEncounterIsAlwaysAFlashcard() {
        assertEquals(ReviewMode.FLASHCARD, reviewModeFor(0))
    }

    @Test
    fun everyDrillAppearsOncePerRotation() {
        val oneCycle = (0..4).map(::reviewModeFor)
        assertEquals(
            setOf(
                ReviewMode.FLASHCARD,
                ReviewMode.FILL_IN_BLANK,
                ReviewMode.FIRST_LETTERS,
                ReviewMode.REVERSE,
                ReviewMode.TYPE_WHOLE,
            ),
            oneCycle.toSet(),
        )
    }

    @Test
    fun rotationRampsUpThenRepeats() {
        assertEquals(ReviewMode.FILL_IN_BLANK, reviewModeFor(1))
        assertEquals(ReviewMode.TYPE_WHOLE, reviewModeFor(4))
        assertEquals(ReviewMode.FLASHCARD, reviewModeFor(5))
    }

    // ---- first letters ----

    @Test
    fun verseBecomesItsInitials() {
        assertEquals("F G l t w", firstLetters("  For   God loved the world "))
    }

    // ---- blank scaling ----

    @Test
    fun blanksGrowWithReviewsUpToACap() {
        assertEquals(3, maxBlanksFor(1))
        assertEquals(8, maxBlanksFor(6))
        assertEquals(8, maxBlanksFor(100))
    }

    // ---- typed verse check ----

    @Test
    fun perfectMatchCountsEveryWord() {
        val result = checkTypedVerse("For God loved the world", "For God loved the world")
        assertEquals(5, result.matchedCount)
        assertEquals(5, result.isCorrect.count { it })
    }

    @Test
    fun caseAndPunctuationDoNotMatter() {
        val result = checkTypedVerse("For God loved the world.", "for god LOVED, the world")
        assertEquals(5, result.matchedCount)
    }

    @Test
    fun trailingWordsMissWhenTypedTextIsShort() {
        val result = checkTypedVerse("For God loved the world", "For God")
        assertEquals(listOf(true, true, false, false, false), result.isCorrect)
        assertEquals(2, result.matchedCount)
    }

    @Test
    fun wrongWordsAreFlaggedInPlace() {
        val result = checkTypedVerse("For God loved the world", "For God hated the world")
        assertEquals(listOf(true, true, false, true, true), result.isCorrect)
    }

    // ---- normalization ----

    @Test
    fun normalizeStripsCaseAndPunctuation() {
        assertEquals("love", normalizeWord("“Love,”"))
        assertEquals("jesus", normalizeWord(" Jesus. "))
    }
}