package app.scripture

import app.scripture.data.InMemoryDataStorage
import app.scripture.data.ScriptureData
import app.scripture.data.VerseRepository
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.ReviewProgress
import app.scripture.domain.ReviewStage
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
    fun ladderStageRoundTrips() {
        val storage = InMemoryDataStorage()
        val data = ScriptureData(
            verses = listOf(
                Verse(
                    id = "id-2",
                    reference = "Isaiah 25:8",
                    text = "He will destroy death forever…",
                    createdEpochDay = 20_000,
                    progress = ReviewProgress(stage = ReviewStage.CLOZE_90),
                ),
            ),
        )

        VerseRepository(storage).save(data)

        assertEquals(
            ReviewStage.CLOZE_90,
            VerseRepository(storage).load().verses.single().progress.stage,
        )
    }

    @Test
    fun preLadderFilesLoadWithANullStage() {
        // A file written before the ladder existed has progress but no
        // `stage` — null is the marker the store migrates from.
        val preLadderJson = """
            {
              "verses": [
                { "id": "old-2", "reference": "John 3:16", "text": "For God loved the world…",
                  "createdEpochDay": 19000,
                  "progress": { "repetitions": 3, "easiness": 2.5, "intervalDays": 15, "dueEpochDay": 19020 } }
              ],
              "history": []
            }
        """.trimIndent()

        val storage = InMemoryDataStorage().apply { write(dataFile, preLadderJson) }
        val progress = VerseRepository(storage).load().verses.single().progress

        assertEquals(null, progress.stage)
        assertEquals(3, progress.repetitions)
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