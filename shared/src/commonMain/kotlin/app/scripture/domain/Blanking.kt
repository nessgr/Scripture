package app.scripture.domain

import kotlin.math.ceil
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
 * Choose which words to blank out for cloze review.
 *
 * [fraction] is how much of the verse goes blank — 0.3, 0.6, 0.9 as the verse
 * climbs the ladder. Content words (nouns, verbs) blank first, because they
 * carry the verse's meaning and grammar can't scaffold them away; function
 * words only start disappearing once the drill demands most of the verse.
 *
 * Deterministic for a given seed: the same seed always blanks the same words,
 * so one review round is stable, while the next round (new seed) feels fresh.
 */
fun makeFillInBlank(text: String, seed: Long, fraction: Double): FillInBlank {
    if (text.isBlank()) return FillInBlank(emptyList(), emptyList())
    val tokens = text.trim().split(Regex("\\s+"))

    val contentWords = tokens.indices.filter { isContentWord(tokens[it]) }
    val functionWords = tokens.indices.filterNot { it in contentWords }

    // Blank priority: content words first, then function words — each group
    // shuffled so the same fraction doesn't always hit the same words.
    val ranked = contentWords.shuffled(Random(seed)) + functionWords.shuffled(Random(seed + 1))

    val blankCount = ceil(fraction * tokens.size)
        .toInt()
        .coerceIn(0, ranked.size)

    return FillInBlank(
        tokens = tokens,
        blankedIndexes = ranked.take(blankCount).sorted(),
    )
}

/** A word worth blanking: real letters, long enough to recall, not filler. */
private fun isContentWord(word: String): Boolean {
    val bare = word.lowercase()
        .trim('.', ',', ';', ':', '!', '?', '"', '“', '”', '‘', '’')
    return bare.length >= 4 && bare.all { it.isLetter() } && bare !in STOP_WORDS
}