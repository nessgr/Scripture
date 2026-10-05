package app.scripture.domain

import kotlin.random.Random

/**
 * A verse chopped into words, with the words to type back marked by index.
 */
data class FillInBlank(
    val tokens: List<String>,
    val blankedIndexes: List<Int>,
)

/** Small words that carry no memorization value. */
private val STOP_WORDS = setOf(
    "that", "this", "with", "from", "have", "them", "they", "their", "when",
    "then", "into", "unto", "because", "your", "will", "shall", "which", "these",
    "those", "there", "been", "were", "over", "after", "before",
)

/**
 * Choose which words to blank out for fill-in-the-blank review.
 *
 * Deterministic for a given seed: the same seed always blanks the same words,
 * so one review round is stable, while the next round (new seed) feels fresh.
 * Candidates are real words of 4+ letters; filler words are never blanked.
 */
fun makeFillInBlank(text: String, seed: Long, maxBlanks: Int = 2): FillInBlank {
    val tokens = text.trim().split(Regex("\\s+"))

    val candidates = tokens.indices.filter { i ->
        val word = tokens[i]
            .lowercase()
            .trim('.', ',', ';', ':', '!', '?', '"', '“', '”', '‘', '’')
        word.length >= 4 && word.all { it.isLetter() } && word !in STOP_WORDS
    }

    val blanked = candidates
        .shuffled(Random(seed))
        .take(maxBlanks.coerceAtLeast(0))
        .sorted()

    return FillInBlank(tokens = tokens, blankedIndexes = blanked)
}