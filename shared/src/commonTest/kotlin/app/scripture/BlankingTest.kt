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
        val a = makeFillInBlank(longText, seed = 7)
        val b = makeFillInBlank(longText, seed = 7)
        assertEquals(a.blankedIndexes, b.blankedIndexes)
    }

    @Test
    fun blanksRealWordsNotFiller() {
        // Tokens: For(3) God(3) loved(5) the(stop) world(5) so(2) much(4)
        //         that(stop) he(2) gave(4) his(3) only(4) begotten(8) Son(3)
        // Candidates: loved, world, much, gave, only, begotten
        val candidates = listOf(2, 4, 6, 9, 11, 12)
        val puzzle = makeFillInBlank(longText, seed = 1, maxBlanks = 6)
        assertEquals(candidates.sorted(), puzzle.blankedIndexes.sorted())
    }

    @Test
    fun respectsMaxBlanks() {
        val puzzle = makeFillInBlank(longText, seed = 1, maxBlanks = 1)
        assertEquals(1, puzzle.blankedIndexes.size)
    }

    @Test
    fun shortTextGracesfullyBlanksLess() {
        // Only "love" qualifies (God=3 letters, is=2)
        val puzzle = makeFillInBlank("God is love", seed = 1, maxBlanks = 2)
        assertEquals(1, puzzle.blankedIndexes.size)
        assertEquals(2, puzzle.blankedIndexes.first())
    }

    @Test
    fun differentSeedsProduceDifferentPuzzles() {
        val puzzles = (1L..20L).map { seed ->
            makeFillInBlank(longText, seed = seed).blankedIndexes
        }
        assertTrue(puzzles.distinct().size > 1, "20 seeds should not all agree")
    }
}