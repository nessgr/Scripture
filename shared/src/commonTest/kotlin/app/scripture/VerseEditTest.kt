package app.scripture

import app.scripture.domain.ReviewProgress
import app.scripture.domain.Verse
import app.scripture.domain.edited
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerseEditTest {

    private val verse = Verse(
        id = "abc",
        reference = "John 3:16",
        text = "For God loved the world",
        tags = listOf("love"),
        translation = "NWT",
        createdEpochDay = 19000,
        progress = ReviewProgress(repetitions = 4, intervalDays = 12, dueEpochDay = 19012),
    )

    @Test
    fun editChangesContentButPreservesIdentityAndProgress() {
        val edited = verse.edited("John 3:17", "new text", listOf("hope"), "NIV")

        assertEquals("abc", edited.id)
        assertEquals(19000, edited.createdEpochDay)
        assertEquals(verse.progress, edited.progress)
        assertEquals("John 3:17", edited.reference)
        assertEquals("new text", edited.text)
        assertEquals(listOf("hope"), edited.tags)
        assertEquals("NIV", edited.translation)
    }

    @Test
    fun editTrimsFieldsAndDropsBlankTags() {
        val edited = verse.edited("  John 3:16  ", "  text  ", listOf(" love ", "", " faith"), "  ")
        assertEquals("John 3:16", edited.reference)
        assertEquals("text", edited.text)
        assertEquals(listOf("love", "faith"), edited.tags)
        assertNull(edited.translation)
    }

    @Test
    fun editKeepsNullTranslationNull() {
        val noTranslation = verse.copy(translation = null)
        assertNull(noTranslation.edited("R", "T", emptyList(), null).translation)
    }

    @Test
    fun editKeepsExistingProgressUntouched() {
        val edited = verse.edited("R", "T", emptyList(), null)
        assertTrue(edited.progress === verse.progress)
    }
}