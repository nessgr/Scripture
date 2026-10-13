package house.glyph

import house.glyph.domain.GuessState
import house.glyph.domain.Words
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameStoreTest {

    /** A seed that hides "about" — deterministic tests need a known word. */
    private val aboutSeed = Words.answers.indexOf("about")

    @Test
    fun `a new game hides the seeded word and starts clean`() {
        val games = GameStore(InMemoryDataStorage(), StatsStore(InMemoryDataStorage()))
        games.newGame(aboutSeed)
        assertEquals("about", games.game.value.answer)
        assertTrue(games.game.value.guesses.isEmpty())
    }

    @Test
    fun `a round resumes exactly where it was left`() {
        val storage = InMemoryDataStorage()
        val stats = StatsStore(storage)
        val first = GameStore(storage, stats)
        first.newGame(aboutSeed)
        first.submit("crane")

        val second = GameStore(storage, stats)
        assertEquals("about", second.game.value.answer)
        assertEquals(listOf("crane"), second.game.value.guesses.map { it.word })
        assertEquals(GuessState.PLAYING, second.game.value.state)
        // Resuming is not playing — nothing was recorded.
        assertEquals(0, stats.stats.value.played)
    }

    @Test
    fun `a win is recorded once, in the right distribution slot`() {
        val storage = InMemoryDataStorage()
        val stats = StatsStore(storage)
        val games = GameStore(storage, stats)
        games.newGame(aboutSeed)
        games.submit("crane")
        games.submit("about")

        val record = stats.stats.value
        assertEquals(1, record.played)
        assertEquals(1, record.wins)
        assertEquals(1, record.streak)
        assertEquals(listOf(0, 1, 0, 0, 0, 0), record.distribution)

        // The finished board is frozen: more guesses bounce without touching stats.
        games.submit("crane")
        assertEquals(1, stats.stats.value.played)
    }

    @Test
    fun `six misses lose the round and reset the streak`() {
        val storage = InMemoryDataStorage()
        val stats = StatsStore(storage)
        stats.recordWin(1) // a streak to lose

        val games = GameStore(storage, stats)
        games.newGame(aboutSeed)
        val strays = Words.answers.filter { it != "about" }.take(6)
        repeat(5) { games.submit(strays[it]) }
        assertEquals(GuessState.PLAYING, games.game.value.state)
        games.submit(strays[5])

        assertEquals(GuessState.LOST, games.game.value.state)
        assertEquals(2, stats.stats.value.played)
        assertEquals(0, stats.stats.value.streak)
        assertEquals(1, stats.stats.value.bestStreak)
    }

    @Test
    fun `a corrupt round file means a fresh game`() {
        val storage = InMemoryDataStorage()
        storage.write("glyph-round.json", "{not json")
        val games = GameStore(storage, StatsStore(storage))
        assertTrue(games.game.value.guesses.isEmpty())
        assertTrue(games.game.value.answer in Words.answers)
    }

    @Test
    fun `a hand-edited round with bogus guesses cannot be trusted`() {
        val storage = InMemoryDataStorage()
        storage.write("glyph-round.json", """{"answer":"about","guesses":["xqzjv","crane"]}""")
        val games = GameStore(storage, StatsStore(storage))
        assertTrue(games.game.value.guesses.isEmpty())
    }

    @Test
    fun `resuming a finished round shows the result without recording it again`() {
        val storage = InMemoryDataStorage()
        storage.write("glyph-round.json", """{"answer":"about","guesses":["about"]}""")
        val stats = StatsStore(storage)
        val games = GameStore(storage, stats)

        assertEquals(GuessState.WON, games.game.value.state)
        assertEquals(0, stats.stats.value.played)
    }

    @Test
    fun `a new word is not the word just played`() {
        val games = GameStore(InMemoryDataStorage(), StatsStore(InMemoryDataStorage()))
        games.newGame(aboutSeed)
        val firstAnswer = games.game.value.answer
        games.newGame(aboutSeed) // same seed would pick the same word — the store must not let it
        assertNotEquals(firstAnswer, games.game.value.answer)
    }
}