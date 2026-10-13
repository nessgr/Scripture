package app.scripture

import app.scripture.domain.makeFillInBlank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlankingTest {

    private val longText =
        "For God loved the world so much that he gave his only begotten Son"

    @Test
    fun sameSeedAlwaysBlanksTheSameWords() {
        val a = makeFillInBlank(longText, seed = 7, fraction = 0.5)
        val b = makeFillInBlank(longText, seed = 7, fraction = 0.5)
        assertEquals(a.blankedIndexes, b.blankedIndexes)
    }

    @Test
    fun blanksContentWordsNotFiller() {
        // Tokens: For(3) God(3) loved(5) the(stop) world(5) so(2) much(4)
        //         that(stop) he(2) gave(4) his(3) only(4) begotten(8) Son(3)
        // Content words: loved, world, much, gave, only, begotten
        val candidates = listOf(2, 4, 6, 9, 11, 12)
        // 40% of 14 words = ceil(5.6) = 6 blanks — exactly the content words.
        val puzzle = makeFillInBlank(longText, seed = 1, fraction = 0.4)
        assertEquals(candidates.sorted(), puzzle.blankedIndexes.sorted())
    }

    @Test
    fun theFractionSetsTheBlankCount() {
        val puzzle = makeFillInBlank(longText, seed = 1, fraction = 0.1)
        assertEquals(2, puzzle.blankedIndexes.size) // ceil(0.1 × 14)
    }

    @Test
    fun aShortVerseBlanksItsOneContentWord() {
        // Only "love" qualifies (God=3 letters, is=2)
        val puzzle = makeFillInBlank("God is love", seed = 1, fraction = 0.3)
        assertEquals(listOf(2), puzzle.blankedIndexes)
    }

    @Test
    fun differentSeedsProduceDifferentPuzzles() {
        val puzzles = (1L..20L).map { seed ->
            makeFillInBlank(longText, seed = seed, fraction = 0.5).blankedIndexes
        }
        assertTrue(puzzles.distinct().size > 1, "20 seeds should not all agree")
    }
}