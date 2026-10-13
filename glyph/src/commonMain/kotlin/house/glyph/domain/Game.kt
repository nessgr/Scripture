package house.glyph.domain

/**
 * What one letter of a guess turned out to be:
 * HIT — right letter, right place; CLOSE — right letter, wrong place
 * (the ember mark); MISS — not in the word.
 */
enum class LetterMark { HIT, CLOSE, MISS }

/** One submitted guess: the word and its per-letter marks. */
data class Guess(val word: String, val marks: List<LetterMark>)

enum class GuessState { PLAYING, WON, LOST }

/** Why a guess came back unchanged. */
enum class RejectReason { WRONG_LENGTH, NOT_A_WORD, GAME_OVER }

/** The result of trying a word: either the new game or the reason it bounced. */
sealed interface GuessOutcome {
    data class Accepted(val game: Game) : GuessOutcome
    data class Rejected(val reason: RejectReason) : GuessOutcome
}

const val WORD_LENGTH = 5
const val MAX_GUESSES = 6

/**
 * A round in progress: the hidden word plus every guess so far. The
 * marks are stored, not recomputed, so the grid always shows exactly
 * what was seen when the word went in.
 */
data class Game(
    val answer: String,
    val guesses: List<Guess> = emptyList(),
) {
    val state: GuessState
        get() = when {
            guesses.lastOrNull()?.word == answer -> GuessState.WON
            guesses.size >= MAX_GUESSES -> GuessState.LOST
            else -> GuessState.PLAYING
        }
}

/**
 * The classic two-pass scoring: exact matches are claimed first, then
 * each remaining answer letter can light up at most one more guess
 * position — so duplicates never score twice. Both words must be the
 * same length (the UI enforces [WORD_LENGTH]; the algorithm doesn't care).
 */
fun scoreGuess(guess: String, answer: String): List<LetterMark> {
    val g = guess.lowercase()
    val a = answer.lowercase()
    val marks = MutableList(g.length) { LetterMark.MISS }

    // Pass 1: claim the exact matches, and count what the answer has left to offer.
    val unmatched = mutableMapOf<Char, Int>()
    for (i in g.indices) {
        if (g[i] == a[i]) {
            marks[i] = LetterMark.HIT
        } else {
            unmatched[a[i]] = (unmatched[a[i]] ?: 0) + 1
        }
    }

    // Pass 2: offer the leftovers to the non-exact positions, one each.
    for (i in g.indices) {
        if (marks[i] != LetterMark.MISS) continue
        val left = unmatched[g[i]] ?: 0
        if (left > 0) {
            marks[i] = LetterMark.CLOSE
            unmatched[g[i]] = left - 1
        }
    }
    return marks
}

/**
 * Validate and apply a guess. Returns the updated game, or the reason
 * the word bounced — the caller's game is never changed on a reject.
 */
fun submitGuess(game: Game, word: String): GuessOutcome {
    if (game.state != GuessState.PLAYING) return GuessOutcome.Rejected(RejectReason.GAME_OVER)
    val guess = word.lowercase().trim()
    if (guess.length != WORD_LENGTH) return GuessOutcome.Rejected(RejectReason.WRONG_LENGTH)
    if (!Words.isValidGuess(guess)) return GuessOutcome.Rejected(RejectReason.NOT_A_WORD)
    val next = game.copy(guesses = game.guesses + Guess(guess, scoreGuess(guess, game.answer)))
    return GuessOutcome.Accepted(next)
}

/**
 * A letter's best mark across every guess so far (HIT beats CLOSE beats
 * MISS) — what the on-screen keyboard key should look like. Null when the
 * letter hasn't been tried yet.
 */
fun bestMark(guesses: List<Guess>, letter: Char): LetterMark? {
    val rank = listOf(LetterMark.MISS, LetterMark.CLOSE, LetterMark.HIT)
    val target = letter.lowercaseChar()
    return guesses
        .flatMap { guess -> guess.word.indices.map { guess.word[it] to guess.marks[it] } }
        .filter { (l, _) -> l == target }
        .maxByOrNull { (_, mark) -> rank.indexOf(mark) }
        ?.second
}