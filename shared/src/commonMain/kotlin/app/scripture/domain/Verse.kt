package app.scripture.domain

import kotlinx.serialization.Serializable

/**
 * A verse the user is memorizing.
 *
 * Dates are epoch-days (whole days since 1970-01-01), not ISO strings:
 * scheduling stays plain integer math, and the JSON stays small.
 */
@Serializable
data class Verse(
    val id: String,
    val reference: String,              // e.g. "John 3:16"
    val text: String,
    val tags: List<String> = emptyList(),
    val translation: String? = null,   // e.g. "NWT" — purely informational
    val createdEpochDay: Long,
    val progress: ReviewProgress = ReviewProgress(),
)