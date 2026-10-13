package app.scripture

import app.scripture.domain.FillInBlank
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.ReviewProgress
import app.scripture.domain.ReviewStage
import app.scripture.domain.WordStatus
import app.scripture.domain.checkCloze
import app.scripture.domain.checkTypedVerse
import app.scripture.domain.clozeFraction
import app.scripture.domain.firstLetters
import app.scripture.domain.makeFillInBlank
import app.scripture.domain.migrated
import app.scripture.domain.nextStage
import app.scripture.domain.normalizeWord
import app.scripture.domain.reviewModeFor
import app.scripture.domain.suggestGrade
import kotlin.test.Test
import kotlin.test.assertEquals

class DrillsTest {

    // ---- the ladder ----

    @Test
    fun goodAndEasyClimbOneRungAtATime() {
        assertEquals(ReviewStage.CLOZE_30, nextStage(ReviewStage.READ, Grade.GOOD))
        assertEquals(ReviewStage.CLOZE_60, nextStage(ReviewStage.CLOZE_30, Grade.EASY))
        assertEquals(ReviewStage.REVERSE, nextStage(ReviewStage.FIRST_LETTERS, Grade.GOOD))
    }

    @Test
    fun theTopOfTheLadderIsACeiling() {
        assertEquals(ReviewStage.FULL_RECALL, nextStage(ReviewStage.FULL_RECALL, Grade.GOOD))
        assertEquals(ReviewStage.FULL_RECALL, nextStage(ReviewStage.FULL_RECALL, Grade.EASY))
    }

    @Test
    fun againDropsOneRungButNotBelowTheBottom() {
        assertEquals(ReviewStage.CLOZE_60, nextStage(ReviewStage.CLOZE_90, Grade.AGAIN))
        assertEquals(ReviewStage.READ, nextStage(ReviewStage.READ, Grade.AGAIN))
    }

    @Test
    fun hardHoldsTheRung() {
        assertEquals(ReviewStage.CLOZE_90, nextStage(ReviewStage.CLOZE_90, Grade.HARD))
    }

    @Test
    fun clozeRungsBlankThirtySixtyNinetyPercent() {
        assertEquals(0.3, clozeFraction(ReviewStage.CLOZE_30))
        assertEquals(0.6, clozeFraction(ReviewStage.CLOZE_60))
        assertEquals(0.9, clozeFraction(ReviewStage.CLOZE_90))
        assertEquals(0.0, clozeFraction(ReviewStage.READ))
    }

    @Test
    fun everyRungKnowsItsDrill() {
        assertEquals(ReviewMode.FLASHCARD, reviewModeFor(ReviewStage.READ))
        assertEquals(ReviewMode.FILL_IN_BLANK, reviewModeFor(ReviewStage.CLOZE_60))
        assertEquals(ReviewMode.FIRST_LETTERS, reviewModeFor(ReviewStage.FIRST_LETTERS))
        assertEquals(ReviewMode.REVERSE, reviewModeFor(ReviewStage.REVERSE))
        assertEquals(ReviewMode.TYPE_WHOLE, reviewModeFor(ReviewStage.FULL_RECALL))
    }

    @Test
    fun preLadderVersesArePlacedByTheirRepetitionCount() {
        assertEquals(ReviewStage.READ, ReviewProgress().migrated().stage)
        assertEquals(
            ReviewStage.CLOZE_30,
            ReviewProgress(repetitions = 1).migrated().stage,
        )
        assertEquals(
            ReviewStage.CLOZE_90,
            ReviewProgress(repetitions = 3).migrated().stage,
        )
        assertEquals(
            ReviewStage.FULL_RECALL,
            ReviewProgress(repetitions = 20).migrated().stage,
        )
    }

    @Test
    fun alreadyPlacedVersesAreLeftAlone() {
        val progress = ReviewProgress(repetitions = 20, stage = ReviewStage.CLOZE_30)
        assertEquals(progress, progress.migrated())
    }

    // ---- first letters ----

    @Test
    fun verseBecomesItsInitials() {
        assertEquals("F G l t w", firstLetters("  For   God loved the world "))
    }

    // ---- cloze ----

    @Test
    fun fractionScalesTheBlankCount() {
        val verse = (1..20).joinToString(" ") { "word$it" } // 20 content words

        val at30 = makeFillInBlank(verse, seed = 1, fraction = 0.3)
        val at90 = makeFillInBlank(verse, seed = 1, fraction = 0.9)

        assertEquals(6, at30.blankedIndexes.size) // ceil(0.3 × 20)
        assertEquals(18, at90.blankedIndexes.size) // ceil(0.9 × 20)
    }

    @Test
    fun contentWordsBlankBeforeFunctionWords() {
        // Two content words, two function words — 50% must take the content.
        val puzzle = makeFillInBlank("beloved remembered when there", seed = 42, fraction = 0.5)

        assertEquals(setOf(0, 1), puzzle.blankedIndexes.toSet())
    }

    @Test
    fun functionWordsOnlyBlankWhenTheFractionDemandsThem() {
        val puzzle = makeFillInBlank("beloved remembered when there", seed = 42, fraction = 1.0)

        assertEquals(setOf(0, 1, 2, 3), puzzle.blankedIndexes.toSet())
    }

    @Test
    fun sameSeedBlanksTheSameWords() {
        val verse = (1..10).joinToString(" ") { "word$it" }
        val a = makeFillInBlank(verse, seed = 7, fraction = 0.5)
        val b = makeFillInBlank(verse, seed = 7, fraction = 0.5)

        assertEquals(a, b)
    }

    @Test
    fun aTinyVerseStillGetsABlankToType() {
        val puzzle = makeFillInBlank("I am", seed = 1, fraction = 0.3)
        assertEquals(1, puzzle.blankedIndexes.size)
    }

    @Test
    fun emptyTextBlanksNothing() {
        assertEquals(emptyList(), makeFillInBlank("", seed = 1, fraction = 0.3).blankedIndexes)
    }

    // ---- typed verse check ----

    @Test
    fun perfectMatchIsFullyCorrect() {
        val result = checkTypedVerse("For God loved the world", "For God loved the world")
        assertEquals(5, result.statuses.count { it == WordStatus.CORRECT })
        assertEquals(1.0, result.accuracy)
    }

    @Test
    fun caseAndPunctuationDoNotMatter() {
        val result = checkTypedVerse("For God loved the world.", "for god LOVED, the world")
        assertEquals(1.0, result.accuracy)
    }

    @Test
    fun aSwappedPairOfLettersIsANearMiss() {
        // Transposition — the most common typo of all — is one edit.
        val result = checkTypedVerse("beloved world", "belvoed world")
        assertEquals(WordStatus.NEAR, result.statuses[0])
        assertEquals(0.75, result.accuracy)
    }

    @Test
    fun aDeletionIsANearMiss() {
        val result = checkTypedVerse("beloved world", "belovd world")
        assertEquals(WordStatus.NEAR, result.statuses[0])
    }

    @Test
    fun shortWordsGetNoNearMissCredit() {
        // "cat" → "cot": one substitution, but a short word's near-miss is a miss.
        val result = checkTypedVerse("cat", "cot")
        assertEquals(listOf(WordStatus.WRONG), result.statuses)
        assertEquals(0.0, result.accuracy)
    }

    @Test
    fun trailingWordsAreWrongWhenTypedTextIsShort() {
        val result = checkTypedVerse("For God loved the world", "For God")
        assertEquals(
            listOf(WordStatus.CORRECT, WordStatus.CORRECT, WordStatus.WRONG, WordStatus.WRONG, WordStatus.WRONG),
            result.statuses,
        )
        assertEquals(0.4, result.accuracy)
    }

    @Test
    fun wrongWordsAreFlaggedInPlace() {
        val result = checkTypedVerse("For God loved the world", "For God hated the world")
        assertEquals(
            listOf(WordStatus.CORRECT, WordStatus.CORRECT, WordStatus.WRONG, WordStatus.CORRECT, WordStatus.CORRECT),
            result.statuses,
        )
    }

    // ---- cloze check ----

    @Test
    fun clozeChecksOnlyTheBlankedWords() {
        val puzzle = FillInBlank(
            tokens = listOf("For", "God", "loved", "the", "world"),
            blankedIndexes = listOf(1, 4),
        )
        val result = checkCloze(puzzle, "God world")

        assertEquals(listOf("God", "world"), result.tokens)
        assertEquals(1.0, result.accuracy)
    }

    @Test
    fun clozeCountsAMistypedBlank() {
        val puzzle = FillInBlank(
            tokens = listOf("For", "God", "loved", "the", "world"),
            blankedIndexes = listOf(1, 4),
        )
        val result = checkCloze(puzzle, "God wolrd")

        assertEquals(WordStatus.NEAR, result.statuses[1])
        assertEquals(0.75, result.accuracy)
    }

    // ---- suggested grade ----

    @Test
    fun accuracySuggestsTheHonestGrade() {
        assertEquals(Grade.EASY, suggestGrade(1.0))
        assertEquals(Grade.GOOD, suggestGrade(0.9))
        assertEquals(Grade.HARD, suggestGrade(0.6))
        assertEquals(Grade.AGAIN, suggestGrade(0.3))
    }

    // ---- normalization ----

    @Test
    fun normalizeStripsCaseAndPunctuation() {
        assertEquals("love", normalizeWord("“Love,”"))
        assertEquals("jesus", normalizeWord(" Jesus. "))
    }
}