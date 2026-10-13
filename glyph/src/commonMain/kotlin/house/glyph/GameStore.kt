package house.glyph

import house.glyph.data.DataStorage
import house.glyph.domain.Game
import house.glyph.domain.GuessState
import house.glyph.domain.Guess
import house.glyph.domain.GuessOutcome
import house.glyph.domain.MAX_GUESSES
import house.glyph.domain.Words
import house.glyph.domain.scoreGuess
import house.glyph.domain.submitGuess
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * The round in progress and everything a round touches: it resumes the
 * persisted game at construction (so closing the app mid-round loses
 * nothing), validates each guess through the domain, writes the round
 * through after every change, and hands the finished round to
 * [stats] exactly once.
 */
class GameStore(
    private val storage: DataStorage,
    private val stats: StatsStore,
) {

    private val _game = MutableStateFlow(read() ?: freshGame())
    val game: StateFlow<Game> = _game.asStateFlow()

    /** A new hidden word. The seed decides which — callers may pass one, players don't. */
    fun newGame(seed: Int = Random.nextInt()) {
        var answer = Words.randomAnswer(seed)
        var s = seed
        while (answer == _game.value.answer && Words.answers.size > 1) answer = Words.randomAnswer(++s)
        _game.value = Game(answer)
        write()
    }

    /**
     * Try a word. Rejections leave the round untouched; an accepted
     * guess updates the board, and a round that just ended is recorded.
     */
    fun submit(word: String): GuessOutcome {
        val before = _game.value
        val outcome = submitGuess(before, word)
        if (outcome is GuessOutcome.Accepted) {
            _game.value = outcome.game
            val after = outcome.game
            if (after.state != GuessState.PLAYING) recordResult(after)
            write()
        }
        return outcome
    }

    private fun recordResult(game: Game) {
        if (game.state == GuessState.WON) {
            stats.recordWin(game.guesses.size)
        } else {
            stats.recordLoss()
        }
    }

    /** Persist just the identity of the round — marks are recomputed from the answer on resume. */
    private fun write() {
        val game = _game.value
        val round = RoundData(
            answer = game.answer,
            guesses = game.guesses.map { it.word },
        )
        storage.write(ROUND_BLOB, SettingsStore.json.encodeToString(RoundData.serializer(), round))
    }

    /** Null when nothing usable is stored — a fresh game replaces it. */
    private fun read(): Game? {
        val round = storage.read(ROUND_BLOB)
            ?.let { text -> runCatching { SettingsStore.json.decodeFromString<RoundData>(text) }.getOrNull() }
            ?: return null
        if (round.answer !in Words.answers) return null
        val guesses = round.guesses
            .filter { it in Words.answers || Words.isValidGuess(it) }  // must be replayable
            .take(MAX_GUESSES)
        if (guesses.size != round.guesses.size) return null  // a hand-edited file can't be trusted
        return Game(round.answer, guesses.map { word -> Guess(word, scoreGuess(word, round.answer)) })
    }

    /** The fallback round — a random word nobody has guessed at yet. */
    private fun freshGame(): Game = Game(Words.randomAnswer(Random.nextInt()))

    companion object {
        private const val ROUND_BLOB = "glyph-round.json"
    }
}

/** The persisted round: the answer and the guess words, in order. */
@Serializable
private data class RoundData(
    val answer: String,
    val guesses: List<String> = emptyList(),
)