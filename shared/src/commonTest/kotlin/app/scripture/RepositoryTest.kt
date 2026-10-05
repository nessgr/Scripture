package app.scripture

import app.scripture.data.InMemoryDataStorage
import app.scripture.data.ScriptureData
import app.scripture.data.VerseRepository
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.ReviewProgress
import app.scripture.domain.ReviewRecord
import app.scripture.domain.Verse
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryTest {

    // Must match VerseRepository's private file name — deliberately spelled
    // out so this test breaks loudly if the file name ever changes.
    private val dataFile = "scripture-data.json"

    @Test
    fun saveThenLoadRoundTripsExactly() {
        val storage = InMemoryDataStorage()
        val data = ScriptureData(
            verses = listOf(
                Verse(
                    id = "id-1",
                    reference = "John 3:16",
                    text = "For God loved the world…",
                    tags = listOf("love"),
                    translation = "NWT",
                    createdEpochDay = 20_000,
                    progress = ReviewProgress(repetitions = 2, intervalDays = 6, dueEpochDay = 20_010),
                ),
            ),
            history = listOf(ReviewRecord("id-1", 20_005, Grade.GOOD, ReviewMode.FLASHCARD)),
        )

        VerseRepository(storage).save(data)

        // A brand-new repository instance proves the data survives the trip.
        assertEquals(data, VerseRepository(storage).load())
    }

    @Test
    fun filesWrittenByV1StillLoadWithDefaults() {
        // A v1 file has no `progress`, `tags`, or `translation` fields.
        val v1Json = """
            {
              "verses": [
                { "id": "old-1", "reference": "Psalm 1:1", "text": "Happy is the man…", "createdEpochDay": 19000 }
              ],
              "history": []
            }
        """.trimIndent()

        val storage = InMemoryDataStorage().apply { write(dataFile, v1Json) }
        val loaded = VerseRepository(storage).load()

        assertEquals(1, loaded.verses.size)
        assertEquals(ReviewProgress(), loaded.verses.first().progress)
        assertEquals(emptyList(), loaded.verses.first().tags)
    }

    @Test
    fun corruptedFileStartsFreshInsteadOfCrashing() {
        val storage = InMemoryDataStorage().apply { write(dataFile, "{ not json at all") }
        assertEquals(ScriptureData(), VerseRepository(storage).load())
    }

    @Test
    fun missingFileStartsFresh() {
        assertEquals(ScriptureData(), VerseRepository(InMemoryDataStorage()).load())
    }
}