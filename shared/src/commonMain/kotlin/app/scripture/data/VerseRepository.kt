package app.scripture.data

import app.scripture.domain.ReviewRecord
import app.scripture.domain.Verse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the app persists, in one JSON document per file. */
@Serializable
data class ScriptureData(
    val verses: List<Verse> = emptyList(),
    val history: List<ReviewRecord> = emptyList(),
)

/**
 * Loads and saves [ScriptureData] as JSON text via a [DataStorage].
 *
 * Schema evolution rule: any new field added to [Verse] or [ScriptureData]
 * MUST have a default so older saved files keep loading. `ignoreUnknownKeys`
 * protects the other direction — files written by a newer app version.
 */
class VerseRepository(private val storage: DataStorage) {

    private val json = Json {
        ignoreUnknownKeys = true   // tolerate fields added by newer versions
        encodeDefaults = true      // defaults round-trip, so files are complete
        prettyPrint = true          // humans can peek at the file
    }

    fun load(): ScriptureData =
        storage.read(FILE_NAME)
            ?.let { text ->
                runCatching { json.decodeFromString<ScriptureData>(text) }
                    .getOrElse {
                        // A corrupted file must not brick the app — start fresh.
                        ScriptureData()
                    }
            }
            ?: ScriptureData()

    fun save(data: ScriptureData) {
        storage.write(FILE_NAME, json.encodeToString(ScriptureData.serializer(), data))
    }

    companion object {
        private const val FILE_NAME = "scripture-data.json"
    }
}

/**
 * In-memory [DataStorage] for tests (and for the seeded preview build):
 * read returns exactly what was written, nothing touches the disk.
 */
class InMemoryDataStorage : DataStorage {
    private val files = mutableMapOf<String, String>()

    override fun read(name: String): String? = files[name]

    override fun write(name: String, content: String) {
        files[name] = content
    }
}