package app.scripture.domain

/**
 * Phrase chunking — the seam every phrase-level drill builds on. The
 * scramble and finish-the-verse drills (Phase 2) chunk a verse here first;
 * nobody memorizes a verse as one slab of text, so the app shouldn't either.
 *
 * Chunks break at sentence punctuation, tiny fragments merge into the
 * sentence that follows, and a sentence too long to recite in a breath
 * splits again at its commas. Deterministic and pure: same verse, same
 * chunks, on every platform.
 */

private const val CHUNK_MIN_WORDS = 4
private const val CHUNK_MAX_WORDS = 12

fun chunkVerse(text: String): List<String> {
    val sentences = text.trim()
        .split(Regex("(?<=[.?!;:])\\s+"))
        .filter { it.isNotBlank() }
    if (sentences.isEmpty()) return emptyList()

    // Fragments too short to drill alone ride with the sentence that
    // follows; a trailing short one rides with the sentence before it.
    val chunked = mutableListOf<String>()
    var pending = ""
    for (sentence in sentences) {
        val candidate = if (pending.isEmpty()) sentence else "$pending $sentence"
        if (wordCount(candidate) < CHUNK_MIN_WORDS) {
            pending = candidate
        } else {
            chunked.add(candidate)
            pending = ""
        }
    }
    if (pending.isNotEmpty()) {
        if (chunked.isEmpty()) {
            chunked.add(pending)
        } else {
            chunked[chunked.lastIndex] = chunked.last() + " " + pending
            pending = ""
        }
    }

    return chunked.flatMap(::splitLongChunks)
}

/** A chunk past the breath limit splits at its commas — no comma, no split. */
private fun splitLongChunks(chunk: String): List<String> {
    if (wordCount(chunk) <= CHUNK_MAX_WORDS) return listOf(chunk)

    val clauses = chunk.split(Regex("(?<=,)\\s*")).filter { it.isNotBlank() }
    if (clauses.size == 1) return listOf(chunk)

    val chunks = mutableListOf<String>()
    var current = ""
    clauses.forEach { clause ->
        // Clauses end in their comma; the space after it was consumed by
        // the split, so it goes back in here.
        val joined = if (current.isEmpty()) clause else "$current $clause"
        when {
            // Keep growing while the chunk can still hold the clause…
            wordCount(joined) <= CHUNK_MAX_WORDS -> current = joined
            // …or must, because the clause alone would be too small to drill.
            wordCount(current) < CHUNK_MIN_WORDS -> current = joined
            else -> {
                chunks.add(current)
                current = clause
            }
        }
    }
    if (current.isNotEmpty()) chunks.add(current)
    return chunks
}

private fun wordCount(text: String): Int =
    text.trim().split(Regex("\\s+")).count { it.isNotBlank() }