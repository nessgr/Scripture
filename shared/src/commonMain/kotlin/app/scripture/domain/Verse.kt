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
    val collection: String? = null,     // title of the file the verse was imported from
    val createdEpochDay: Long,
    val progress: ReviewProgress = ReviewProgress(),
)

/**
 * Copy with edited content. Id, createdEpochDay, and review progress are
 * preserved — fixing a typo must never reset how well the verse is known.
 * Also the one place content is normalized, so add and edit can't drift.
 */
fun Verse.edited(
    reference: String,
    text: String,
    tags: List<String>,
    translation: String?,
): Verse = copy(
    reference = reference.trim(),
    text = text.trim(),
    tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
    translation = translation?.trim()?.ifEmpty { null },
)