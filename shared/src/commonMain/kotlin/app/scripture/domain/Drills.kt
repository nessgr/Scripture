package app.scripture.domain

/**
 * The drill rotations and their supporting puzzles — everything that turns
 * a verse into a specific memory test. Pure functions, deterministic per
 * (verse, repetitions), so a review is stable while the next one is fresh.
 */

/**
 * Which drill a verse gets, by its successful-review count. The rotation
 * ramps up: reveal first, then puzzles that ask for more of the verse each
 * time, then cycles. AGAIN resets repetitions to 0, which lands a forgotten
 * verse back on the plain flashcard.
 */
private val reviewRotation = listOf(
    ReviewMode.FLASHCARD,
    ReviewMode.FILL_IN_BLANK,
    ReviewMode.FIRST_LETTERS,
    ReviewMode.REVERSE,
    ReviewMode.TYPE_WHOLE,
)

fun reviewModeFor(repetitions: Int): ReviewMode =
    reviewRotation[Math.floorMod(repetitions, reviewRotation.size)]

/**
 * How many words fill-in-the-blank blanks out: starts at the familiar two
 * and grows with the verse's review count, so the drill asks for more of
 * the verse as it becomes better known.
 */
fun maxBlanksFor(repetitions: Int): Int = (2 + repetitions).coerceAtMost(8)

/** The verse as its initials, e.g. "For God loved" → "F G l". */
fun firstLetters(text: String): String =
    text.trim().split(Regex("\\s+")).joinToString(" ") { it.take(1) }

/**
 * Word-by-word verdict on a typed verse. Tokens are the expected verse in
 * order; [isCorrect] says whether the typed text matched that word.
 */
data class TypedVerseCheck(
    val tokens: List<String>,
    val isCorrect: List<Boolean>,
    val matchedCount: Int,
)

fun checkTypedVerse(expected: String, typed: String): TypedVerseCheck {
    val expectedWords = expected.trim().split(Regex("\\s+"))
    val typedWords = typed.trim().split(Regex("\\s+"))

    val isCorrect = expectedWords.mapIndexed { i, word ->
        i < typedWords.size && normalizeWord(typedWords[i]) == normalizeWord(word)
    }
    return TypedVerseCheck(expectedWords, isCorrect, isCorrect.count { it })
}

/** Compare a word leniently: case and surrounding punctuation don't matter. */
fun normalizeWord(word: String): String = word
    .trim()
    .lowercase()
    .trim('.', ',', ';', ':', '!', '?', '"', '“', '”', '‘', '’')