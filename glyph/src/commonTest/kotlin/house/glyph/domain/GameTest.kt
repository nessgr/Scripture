package house.glyph.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTest {

    @Test
    fun `a guess with no shared letters is all misses`() {
        assertEquals(List(5) { LetterMark.MISS }, scoreGuess("crimp", "about"))
    }

    @Test
    fun `the right word is all hits`() {
        assertEquals(List(5) { LetterMark.HIT }, scoreGuess("about", "about"))
        // Case never matters — the board shows one shape either way.
        assertEquals(List(5) { LetterMark.HIT }, scoreGuess("ABOUT", "about"))
    }

    @Test
    fun `right letters in wrong places are closes`() {
        assertEquals(List(5) { LetterMark.CLOSE }, scoreGuess("tabou", "about"))
    }

    @Test
    fun `a doubled guess letter can only claim one answer copy`() {
        // Answer has two b's; the guess has three — the third must miss.
        assertEquals(
            listOf(LetterMark.MISS, LetterMark.HIT, LetterMark.HIT, LetterMark.MISS, LetterMark.CLOSE),
            scoreGuess("bbbba", "abbey"),
        )
    }

    @Test
    fun `a doubled answer letter feeds each guess copy once`() {
        // a close, e close, b hit, a close... spelled out: k-e-b-a-b vs a-b-b-e-y.
        assertEquals(
            listOf(LetterMark.MISS, LetterMark.CLOSE, LetterMark.HIT, LetterMark.CLOSE, LetterMark.CLOSE),
            scoreGuess("kebab", "abbey"),
        )
    }

    @Test
    fun `hits are claimed before closes`() {
        // The a sits at index 3 in the answer; the guess's other a closes.
        val marks = scoreGuess("sassy", "alarm")
        // s-a-s-s-y vs a-l-a-r-m: no exact hits; a's: one in answer at 0, one in
        // guess at 1 → close, second guess a at 3 misses (answer has one a).
        assertEquals(listOf(LetterMark.MISS, LetterMark.CLOSE, LetterMark.MISS, LetterMark.MISS, LetterMark.MISS), marks)
    }

    @Test
    fun `a short word bounces`() {
        val outcome = submitGuess(Game("about"), "abou")
        assertTrue(outcome is GuessOutcome.Rejected)
        assertEquals(RejectReason.WRONG_LENGTH, (outcome as GuessOutcome.Rejected).reason)
    }

    @Test
    fun `a made-up word bounces`() {
        val outcome = submitGuess(Game("about"), "xqzjv")
        assertTrue(outcome is GuessOutcome.Rejected)
        assertEquals(RejectReason.NOT_A_WORD, (outcome as GuessOutcome.Rejected).reason)
    }

    @Test
    fun `a mixed-case real word is accepted`() {
        val outcome = submitGuess(Game("about"), "CrAnE")
        assertTrue(outcome is GuessOutcome.Accepted)
        assertEquals("crane", (outcome as GuessOutcome.Accepted).game.guesses.single().word)
    }

    @Test
    fun `a winning guess ends the game and freezes the board`() {
        var game = Game("about")
        game = (submitGuess(game, "crane") as GuessOutcome.Accepted).game
        assertEquals(GuessState.PLAYING, game.state)

        game = (submitGuess(game, "about") as GuessOutcome.Accepted).game
        assertEquals(GuessState.WON, game.state)
        assertEquals(2, game.guesses.size)

        val after = submitGuess(game, "crane")
        assertTrue(after is GuessOutcome.Rejected)
        assertEquals(RejectReason.GAME_OVER, (after as GuessOutcome.Rejected).reason)
    }

    @Test
    fun `the sixth wrong guess loses`() {
        val strays = Words.answers.filter { it != "about" }.take(6)
        var game = Game("about")
        strays.take(5).forEach { word ->
            game = (submitGuess(game, word) as GuessOutcome.Accepted).game
            assertEquals(GuessState.PLAYING, game.state)
        }
        game = (submitGuess(game, strays[5]) as GuessOutcome.Accepted).game
        assertEquals(GuessState.LOST, game.state)
    }

    @Test
    fun `bestMark prefers a hit over a close over a miss`() {
        val game = Game("about", listOf(Guess("cramp", scoreGuess("cramp", "about"))))
        assertEquals(LetterMark.CLOSE, bestMark(game.guesses, 'a'))   // a is in "about", misplaced
        assertEquals(LetterMark.MISS, bestMark(game.guesses, 'c'))
        assertNull(bestMark(game.guesses, 'z'))

        val won = Game("about", listOf(Guess("about", scoreGuess("about", "about"))))
        assertEquals(LetterMark.HIT, bestMark(won.guesses, 'a'))
    }
}

class WordsTest {

    @Test
    fun `answers are five lowercase letters, sorted, and unique`() {
        assertEquals(5, Words.answers.first().length)
        assertTrue(Words.answers.all { it.length == 5 && it == it.lowercase() && it.all(Char::isLetter) })
        assertEquals(Words.answers, Words.answers.sorted())
        assertEquals(Words.answers.size, Words.answers.toSet().size)
        assertTrue(Words.answers.size > 500, "expected a real dictionary, got ${Words.answers.size}")
    }

    @Test
    fun `every answer is also a legal guess`() {
        assertTrue(Words.answers.all { Words.isValidGuess(it) })
    }

    @Test
    fun `obscure dictionary words are legal guesses but never answers`() {
        assertTrue(Words.isValidGuess("aahed"))
        assertFalse("aahed" in Words.answers)
        assertFalse(Words.isValidGuess("xqzjv"))
    }

    @Test
    fun `randomAnswer follows the seed and survives negative seeds`() {
        assertEquals(Words.answers[7], Words.randomAnswer(7))
        assertEquals(Words.answers[Words.answers.size - 3], Words.randomAnswer(-3))
    }
}