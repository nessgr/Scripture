package app.scripture.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * One row of the simple import format. reference + text are required;
 * unknown keys are ignored, so files written for future versions of the
 * app still import on this one.
 */
@Serializable
data class ImportableVerse(
    val reference: String = "",
    val text: String = "",
    val tags: List<String> = emptyList(),
    val translation: String? = null,
)

/**
 * One row the parser recognized, before it becomes a library verse.
 * A future Full(verse: Verse) variant — for importing the app's own
 * export format — slots in here; toImportedVerses is the only consumer
 * that would grow a branch.
 */
sealed interface ParsedVerse {
    data class Simple(val row: ImportableVerse) : ParsedVerse
}

/**
 * Result of parsing raw import text: rows in file order + how many rows
 * were skipped as invalid. Null means the text wasn't JSON at all.
 */
data class ParsedImport(
    val verses: List<ParsedVerse>,
    val invalidCount: Int,
)

/** What a batch import did — counts drive the summary message. */
sealed interface ImportOutcome {
    data class Imported(
        val addedCount: Int,
        val duplicateCount: Int,
        val invalidCount: Int,
    ) : ImportOutcome
    data object NotJson : ImportOutcome
}

/** The dedupe decision: what gets added and what was skipped as a duplicate. */
data class ImportPlan(
    val versesToAdd: List<Verse>,
    val duplicateCount: Int,
)

private val importJson = Json { ignoreUnknownKeys = true }

/**
 * Parses raw import text. A JSON array is the normal case (one verse per
 * element); a top-level object is treated as a single row so pasting one
 * verse works. Rows that fail to decode or lack reference/text are skipped
 * and counted — one bad row never aborts the batch.
 */
fun parseImportText(text: String): ParsedImport? {
    val element = runCatching { importJson.parseToJsonElement(text) }.getOrNull() ?: return null

    return when (element) {
        is JsonArray -> {
            val rows = element.mapNotNull { decodeRow(it) }
            ParsedImport(rows, invalidCount = element.size - rows.size)
        }
        // A lone object: the paste-one-verse case, and the hook where a
        // future full-library format ({"verses": [...]}) would be detected.
        is JsonObject -> decodeRow(element)?.let { ParsedImport(listOf(it), invalidCount = 0) }
            ?: ParsedImport(emptyList(), invalidCount = 1)
        else -> ParsedImport(emptyList(), invalidCount = 1)
    }
}

private fun decodeRow(element: kotlinx.serialization.json.JsonElement): ParsedVerse.Simple? {
    val row = runCatching { importJson.decodeFromJsonElement(ImportableVerse.serializer(), element) }
        .getOrNull() ?: return null
    if (row.reference.isBlank() || row.text.isBlank()) return null
    return ParsedVerse.Simple(row)
}

/**
 * Turns parsed rows into library verses: each is normalized through
 * [edited] (the same path as add/edit, so imports can't drift), gets a
 * fresh id and review state, and is skipped when the library — or an
 * earlier row in the same batch — already has that reference+translation.
 * File order is preserved.
 */
fun planImport(
    existing: List<Verse>,
    parsed: List<ParsedVerse>,
    todayEpochDay: Long,
    newId: () -> String,
): ImportPlan {
    val seenKeys = existing.map { it.duplicateKey() }.toMutableSet()

    var duplicateCount = 0
    val toAdd = parsed.mapNotNull { candidate ->
        val verse = when (candidate) {
            is ParsedVerse.Simple -> Verse(
                id = newId(),
                reference = candidate.row.reference,
                text = candidate.row.text,
                tags = candidate.row.tags,
                translation = candidate.row.translation,
                createdEpochDay = todayEpochDay,
            ).edited(candidate.row.reference, candidate.row.text, candidate.row.tags, candidate.row.translation)
        }
        if (!seenKeys.add(verse.duplicateKey())) {
            duplicateCount++
            null
        } else {
            verse
        }
    }
    return ImportPlan(versesToAdd = toAdd, duplicateCount = duplicateCount)
}

/** Duplicate identity: same reference and translation, case-insensitive. */
private fun Verse.duplicateKey(): String =
    (reference.trim().lowercase() + "\u0000" + (translation?.trim()?.lowercase() ?: ""))