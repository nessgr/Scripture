package app.scripture

import app.scripture.data.InMemoryDataStorage
import app.scripture.data.VerseRepository
import app.scripture.domain.ImportOutcome
import app.scripture.domain.ImportableVerse
import app.scripture.domain.ParsedVerse
import app.scripture.domain.ReviewProgress
import app.scripture.domain.Verse
import app.scripture.domain.parseImportText
import app.scripture.domain.planImport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerseImportTest {

    private fun existingVerse(id: String, reference: String, translation: String? = null) =
        Verse(id = id, reference = reference, text = "existing text", translation = translation, createdEpochDay = 0)

    private fun rows(text: String) = parseImportText(text)?.verses ?: emptyList()

    // ---- parsing ----

    @Test
    fun parsesSimpleArrayIntoRows() {
        val parsed = parseImportText(
            """
            [
              {"reference": "John 3:16", "text": "For God loved the world", "tags": ["love"], "translation": "NWT"}
            ]
            """.trimIndent()
        )
        assertEquals(
            listOf(
                ParsedVerse.Simple(
                    ImportableVerse("John 3:16", "For God loved the world", listOf("love"), "NWT")
                )
            ),
            parsed?.verses,
        )
        assertEquals(0, parsed?.invalidCount)
    }

    @Test
    fun optionalFieldsDefault() {
        val parsed = parseImportText("""[{"reference": "Psalm 23:1", "text": "Jehovah is my Shepherd"}]""")
        assertEquals(
            listOf(ParsedVerse.Simple(ImportableVerse("Psalm 23:1", "Jehovah is my Shepherd"))),
            parsed?.verses,
        )
    }

    @Test
    fun unknownKeysAreIgnored() {
        val parsed = parseImportText(
            """[{"reference": "John 3:16", "text": "text", "note": "not a real field"}]"""
        )
        assertEquals(1, parsed?.verses?.size)
    }

    @Test
    fun invalidRowsAreSkippedNotFatal() {
        val parsed = parseImportText(
            """
            [
              {"reference": "John 3:16", "text": "text"},
              {"reference": "Psalm 23:1"},
              42
            ]
            """.trimIndent()
        )
        assertEquals(1, parsed?.verses?.size)
        assertEquals(2, parsed?.invalidCount)
    }

    @Test
    fun nonJsonReturnsNull() {
        assertNull(parseImportText("{ not json"))
        assertNull(parseImportText(""))
    }

    @Test
    fun emptyArrayParsesToNothing() {
        val parsed = parseImportText("[]")
        assertEquals(0, parsed?.verses?.size)
        assertEquals(0, parsed?.invalidCount)
    }

    @Test
    fun topLevelObjectTreatedAsSingleRow() {
        val parsed = parseImportText("""{"reference": "John 3:16", "text": "text"}""")
        assertEquals(1, parsed?.verses?.size)
    }

    // ---- planning ----

    private val parsedRows = rows(
        """
        [
          {"reference": "  John 3:16  ", "text": "  For God loved the world  ", "tags": [" love ", "  "], "translation": "  "},
          {"reference": "Psalm 23:1", "text": "Jehovah is my Shepherd"}
        ]
        """.trimIndent()
    )

    @Test
    fun rowsAreNormalized() {
        var next = 0
        val plan = planImport(emptyList(), parsedRows, todayEpochDay = 42) { "id-${next++}" }
        val john = plan.versesToAdd.first()
        assertEquals("John 3:16", john.reference)
        assertEquals("For God loved the world", john.text)
        assertEquals(listOf("love"), john.tags)
        assertEquals(null, john.translation)
    }

    @Test
    fun planAssignsFreshIdDateAndProgress() {
        var next = 0
        val plan = planImport(emptyList(), parsedRows, todayEpochDay = 42) { "id-${next++}" }
        assertEquals(listOf("id-0", "id-1"), plan.versesToAdd.map { it.id })
        assertTrue(plan.versesToAdd.all { it.createdEpochDay == 42L })
        assertTrue(plan.versesToAdd.all { it.progress == ReviewProgress() })
    }

    @Test
    fun duplicatesAgainstLibraryAreSkippedCaseInsensitively() {
        val library = listOf(existingVerse("1", "John 3:16", translation = "NWT"))
        val parsed = rows("""[{"reference": "john 3:16", "text": "text", "translation": "nwt"}]""")
        val plan = planImport(library, parsed, todayEpochDay = 0) { "new" }
        assertTrue(plan.versesToAdd.isEmpty())
        assertEquals(1, plan.duplicateCount)
    }

    @Test
    fun sameReferenceDifferentTranslationIsKept() {
        val library = listOf(existingVerse("1", "John 3:16", translation = "NWT"))
        val parsed = rows("""[{"reference": "John 3:16", "text": "text", "translation": "KJV"}]""")
        val plan = planImport(library, parsed, todayEpochDay = 0) { "new" }
        assertEquals(1, plan.versesToAdd.size)
        assertEquals(0, plan.duplicateCount)
    }

    @Test
    fun duplicatesWithinBatchAreCounted() {
        val parsed = rows(
            """
            [
              {"reference": "John 3:16", "text": "text"},
              {"reference": "JOHN 3:16", "text": "other text"}
            ]
            """.trimIndent()
        )
        val plan = planImport(emptyList(), parsed, todayEpochDay = 0) { "new" }
        assertEquals(1, plan.versesToAdd.size)
        assertEquals(1, plan.duplicateCount)
    }

    @Test
    fun fileOrderIsPreserved() {
        val plan = planImport(emptyList(), parsedRows, todayEpochDay = 0) { "new" }
        assertEquals(listOf("John 3:16", "Psalm 23:1"), plan.versesToAdd.map { it.reference })
    }

    // ---- store integration ----

    @Test
    fun storeImportVersesAddsVersesAndReportsOutcome() {
        val store = ScriptureStore(VerseRepository(InMemoryDataStorage()))
        val outcome = store.importVerses(
            """[{"reference": "John 3:16", "text": "For God loved the world", "tags": ["love"]}]"""
        )
        assertEquals(ImportOutcome.Imported(addedCount = 1, duplicateCount = 0, invalidCount = 0), outcome)
        assertEquals(listOf("John 3:16"), store.verses.value.map { it.reference })
    }

    @Test
    fun storeImportVersesReportsDuplicatesOnSecondRun() {
        val store = ScriptureStore(VerseRepository(InMemoryDataStorage()))
        val text = """[{"reference": "John 3:16", "text": "For God loved the world"}]"""
        store.importVerses(text)
        val second = store.importVerses(text)
        assertEquals(ImportOutcome.Imported(addedCount = 0, duplicateCount = 1, invalidCount = 0), second)
        assertEquals(1, store.verses.value.size)
    }

    @Test
    fun storeImportVersesRejectsNonJson() {
        val store = ScriptureStore(VerseRepository(InMemoryDataStorage()))
        assertEquals(ImportOutcome.NotJson, store.importVerses("{ broken"))
        assertEquals(0, store.verses.value.size)
    }

    @Test
    fun importedIdsAreUnique() {
        val store = ScriptureStore(VerseRepository(InMemoryDataStorage()))
        store.importVerses(
            """
            [
              {"reference": "John 3:16", "text": "a"},
              {"reference": "Psalm 23:1", "text": "b"}
            ]
            """.trimIndent()
        )
        val ids = store.verses.value.map { it.id }
        assertNotEquals(ids[0], ids[1])
    }
}