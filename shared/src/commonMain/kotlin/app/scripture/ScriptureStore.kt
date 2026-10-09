package app.scripture

import app.scripture.data.ScriptureData
import app.scripture.data.VerseRepository
import app.scripture.domain.Grade
import app.scripture.domain.ImportMode
import app.scripture.domain.ImportOutcome
import app.scripture.domain.ReviewMode
import app.scripture.domain.ReviewRecord
import app.scripture.domain.Verse
import app.scripture.domain.buildReviewQueue
import app.scripture.domain.edited
import app.scripture.domain.nextProgress
import app.scripture.domain.parseImportText
import app.scripture.domain.planImport
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
        // Verse.edited is the one place content is normalized — reuse it here
        // so add and edit can never drift apart.
        val verse = Verse(
            id = Uuid.random().toString(),
            reference = reference,
            text = text,
            tags = tags,
            translation = translation,
            createdEpochDay = todayEpochDay(),
        ).edited(reference, text, tags, translation)
        updateVerses { it + verse }
    }

    /**
     * Parses and plans a batch without touching the library — what the
     * Import screen shows before asking add-or-replace. Counts are neutral:
     * only duplicates *within the batch* are skipped, since duplicates
     * against the library only matter for ADD.
     */
    fun previewImport(text: String): ImportOutcome {
        val parsed = parseImportText(text) ?: return ImportOutcome.NotJson
        var next = 0
        val plan = planImport(
            existing = emptyList(),
            parsed = parsed.verses,
            todayEpochDay = todayEpochDay(),
            newId = { "preview-${next++}" }, // discarded — preview never commits
            collection = parsed.title,
        )
        return ImportOutcome.Imported(
            addedCount = plan.versesToAdd.size,
            duplicateCount = plan.duplicateCount,
            invalidCount = parsed.invalidCount,
            title = parsed.title,
        )
    }

    /**
     * Imports verses from simple-JSON text (see domain/VerseImport.kt for the
     * format). Forgiving: rows that duplicate the library or lack
     * reference/text are skipped and reported in the outcome — one bad row
     * never aborts the batch. [ImportMode.REPLACE] swaps the whole library
     * for the batch instead of appending; review history goes with it, for
     * the same reason deleteVerse clears a verse's records.
     */
    fun importVerses(text: String, mode: ImportMode = ImportMode.ADD): ImportOutcome {
        val parsed = parseImportText(text) ?: return ImportOutcome.NotJson
        val plan = planImport(
            existing = if (mode == ImportMode.REPLACE) emptyList() else _verses.value,
            parsed = parsed.verses,
            todayEpochDay = todayEpochDay(),
            newId = { Uuid.random().toString() },
            collection = parsed.title,
        )
        when (mode) {
            ImportMode.ADD -> if (plan.versesToAdd.isNotEmpty()) updateVerses { it + plan.versesToAdd }
            ImportMode.REPLACE -> {
                _verses.value = plan.versesToAdd
                history = emptyList()
                _reviewQueue.value = emptyList()
                persist()
            }
        }
        return ImportOutcome.Imported(
            addedCount = plan.versesToAdd.size,
            duplicateCount = plan.duplicateCount,
            invalidCount = parsed.invalidCount,
            title = parsed.title,
        )
    }

    /**
     * Edits one verse's content. Id, creation date, and review progress are
     * preserved — fixing a typo must never reset how well the verse is known.
     */
    fun updateVerse(
        verseId: String,
        reference: String,
        text: String,
        tags: List<String>,
        translation: String? = null,
    ) {
        updateVerses { verses ->
            verses.map { verse ->
                if (verse.id == verseId) verse.edited(reference, text, tags, translation) else verse
            }
        }
    }

    /**
     * Removes the verse, any queued review of it, and its review history —
     * a history record for a missing verse is a dangling reference every
     * future stats view would have to guard against.
     */
    fun deleteVerse(verseId: String) {
        updateVerses { verses -> verses.filterNot { it.id == verseId } }
        _reviewQueue.value = _reviewQueue.value.filterNot { it.id == verseId }
        history = history.filterNot { it.verseId == verseId }
        persist() // history changed even when the list transform was a no-op
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