package app.scripture

import app.scripture.domain.LibraryFilter
import app.scripture.domain.ReviewProgress
import app.scripture.domain.Verse
import app.scripture.domain.allTags
import app.scripture.domain.filterVerses
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryFilterTest {

    private fun verse(id: String, reference: String, text: String, tags: List<String> = emptyList()) =
        Verse(id = id, reference = reference, text = text, tags = tags, createdEpochDay = 0)

    private val library = listOf(
        verse("1", "John 3:16", "For God loved the world so much that he gave his only begotten Son"),
        verse("2", "Psalm 23:1", "Jehovah is my Shepherd. I will lack nothing.", tags = listOf("comfort")),
        verse("3", "Romans 8:28", "God makes all his works cooperate together for good", tags = listOf("Faith", "comfort")),
    )

    @Test
    fun emptyFilterReturnsListUnchanged() {
        assertEquals(library, filterVerses(library, LibraryFilter()))
        assertEquals(library, filterVerses(library, LibraryFilter(query = "   ")))
    }

    @Test
    fun queryMatchesReferenceCaseInsensitively() {
        val hits = filterVerses(library, LibraryFilter(query = "john"))
        assertEquals(listOf("1"), hits.map { it.id })
    }

    @Test
    fun queryMatchesVerseText() {
        val hits = filterVerses(library, LibraryFilter(query = "SHEPHERD"))
        assertEquals(listOf("2"), hits.map { it.id })
    }

    @Test
    fun queryWithNoMatchesReturnsEmpty() {
        assertTrue(filterVerses(library, LibraryFilter(query = "leviticus")).isEmpty())
    }

    @Test
    fun tagFilterKeepsOnlyVersesCarryingTheTag() {
        val hits = filterVerses(library, LibraryFilter(tag = "comfort"))
        assertEquals(listOf("2", "3"), hits.map { it.id })
    }

    @Test
    fun tagFilterIsCaseInsensitive() {
        val hits = filterVerses(library, LibraryFilter(tag = "FAITH"))
        assertEquals(listOf("3"), hits.map { it.id })
    }

    @Test
    fun combinedQueryAndTagRequiresBoth() {
        val hits = filterVerses(library, LibraryFilter(query = "lack", tag = "comfort"))
        assertEquals(listOf("2"), hits.map { it.id })
    }

    @Test
    fun allTagsIsDistinctAndAlphabetized() {
        // "Faith" keeps its spelling but sorts with, not before, "comfort"
        assertEquals(listOf("comfort", "Faith"), allTags(library))
    }

    @Test
    fun allTagsDedupesCaseInsensitively() {
        val verses = listOf(
            verse("1", "John 3:16", "text", tags = listOf("Faith")),
            verse("2", "Psalm 23:1", "text", tags = listOf("faith", "comfort")),
        )
        assertEquals(listOf("comfort", "Faith"), allTags(verses))
    }

    @Test
    fun allTagsIgnoresBlankTags() {
        val verses = listOf(verse("1", "John 3:16", "text", tags = listOf("  ", "love", "")))
        assertEquals(listOf("love"), allTags(verses))
    }
}