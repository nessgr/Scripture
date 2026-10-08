package app.scripture.domain

/**
 * What the library is currently narrowed to; inactive when both fields are empty.
 */
data class LibraryFilter(
    val query: String = "",
    val tag: String? = null,
) {
    val isActive: Boolean get() = query.isNotBlank() || tag != null
}

/**
 * Case-insensitive search over reference + verse text, AND-combined with an
 * optional tag filter. Order is preserved — the library is already curated.
 */
fun filterVerses(verses: List<Verse>, filter: LibraryFilter): List<Verse> {
    if (!filter.isActive) return verses

    val query = filter.query.trim().lowercase()
    val tag = filter.tag?.lowercase()

    return verses.filter { verse ->
        val matchesQuery = query.isEmpty() ||
            verse.reference.lowercase().contains(query) ||
            verse.text.lowercase().contains(query)
        val matchesTag = tag == null || verse.tags.any { it.lowercase() == tag }
        matchesQuery && matchesTag
    }
}

/**
 * Every distinct tag in the library, alphabetized — feeds the filter chip row.
 * The user's spelling is kept (first seen wins), but duplicates and ordering
 * ignore case so "Faith" and "faith" don't become two chips.
 */
fun allTags(verses: List<Verse>): List<String> =
    verses.flatMap { it.tags }.map { it.trim() }.filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }