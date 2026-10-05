package app.scripture

import app.scripture.data.ScriptureData
import app.scripture.data.VerseRepository
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.ReviewRecord
import app.scripture.domain.Verse
import app.scripture.domain.buildReviewQueue
import app.scripture.domain.nextProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * The app's single state holder — no framework, just observable state.
 *
 * UI reads [verses] / [reviewQueue] (StateFlow) and calls intent methods.
 * Every mutation persists the whole document to disk on a background
 * dispatcher, so the UI thread never touches files.
 */
class ScriptureStore(private val repository: VerseRepository) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _verses = MutableStateFlow<List<Verse>>(emptyList())
    val verses: StateFlow<List<Verse>> = _verses.asStateFlow()

    private val _reviewQueue = MutableStateFlow<List<Verse>>(emptyList())
    val reviewQueue: StateFlow<List<Verse>> = _reviewQueue.asStateFlow()

    private var history: List<ReviewRecord> = emptyList()

    /** Today as days since 1970-01-01 — the currency of all scheduling math. */
    fun todayEpochDay(): Long =
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toEpochDays()
            .toLong()

    /** Call once at startup. Reads the saved document off the UI thread. */
    suspend fun load() {
        val data = withContext(Dispatchers.IO) { repository.load() }
        _verses.value = data.verses
        history = data.history
    }

    fun addVerse(
        reference: String,
        text: String,
        tags: List<String>,
        translation: String? = null,
    ) {
        val verse = Verse(
            id = Uuid.random().toString(),
            reference = reference.trim(),
            text = text.trim(),
            tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
            translation = translation?.trim()?.ifEmpty { null },
            createdEpochDay = todayEpochDay(),
        )
        updateVerses { it + verse }
    }

    fun startReview() {
        _reviewQueue.value = buildReviewQueue(_verses.value, todayEpochDay())
    }

    fun endReview() {
        _reviewQueue.value = emptyList()
    }

    /**
     * Grades the verse at the head of the review queue:
     * reschedules it, logs the attempt, and advances the session.
     * AGAIN keeps it in the session — moved to the back of the line.
     */
    fun gradeCurrent(grade: Grade, mode: ReviewMode) {
        val verse = _reviewQueue.value.firstOrNull() ?: return
        val today = todayEpochDay()

        val graded = verse.copy(progress = nextProgress(verse.progress, grade, today))
        history = history + ReviewRecord(verse.id, today, grade, mode)

        updateVerses { verses ->
            verses.map { if (it.id == verse.id) graded else it }
        }

        _reviewQueue.value = if (grade == Grade.AGAIN) {
            _reviewQueue.value.drop(1) + graded
        } else {
            _reviewQueue.value.drop(1)
        }
    }

    private fun updateVerses(transform: (List<Verse>) -> List<Verse>) {
        _verses.value = transform(_verses.value)
        persist()
    }

    private fun persist() {
        val snapshot = ScriptureData(verses = _verses.value, history = history)
        scope.launch {
            withContext(Dispatchers.IO) { repository.save(snapshot) }
        }
    }
}