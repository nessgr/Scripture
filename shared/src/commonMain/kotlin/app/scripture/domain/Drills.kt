package app.scripture.domain

/**
 * The mastery ladder and its supporting puzzles — everything that turns
 * a verse into a specific memory test. Pure functions, deterministic per
 * (verse, seed), so a review is stable while the next one is fresh.
 */

/** The ladder, bottom rung first. Drills ask for more of the verse each step. */
val masteryLadder = listOf(
    ReviewStage.READ,
    ReviewStage.CLOZE_30,
    ReviewStage.CLOZE_60,
    ReviewStage.CLOZE_90,
    ReviewStage.FIRST_LETTERS,
    ReviewStage.REVERSE,
    ReviewStage.FULL_RECALL,
)

/**
 * Which rung a grade moves a verse to: AGAIN drops one rung (the drill
 * asked too much), HARD holds position, GOOD/EASY climbs one. The top is
 * a ceiling — a mastered verse keeps drilling at full recall, never
 * cycling back to easier tests.
 */
fun nextStage(stage: ReviewStage, grade: Grade): ReviewStage {
    val rung = masteryLadder.indexOf(stage).coerceAtLeast(0)
    return when (grade) {
        Grade.AGAIN -> masteryLadder[maxOf(0, rung - 1)]
        Grade.HARD -> stage
        Grade.GOOD, Grade.EASY -> masteryLadder[minOf(rung + 1, masteryLadder.lastIndex)]
    }
}

/** How much of the verse the cloze rungs blank out. */
fun clozeFraction(stage: ReviewStage): Double = when (stage) {
    ReviewStage.CLOZE_30 -> 0.3
    ReviewStage.CLOZE_60 -> 0.6
    ReviewStage.CLOZE_90 -> 0.9
    else -> 0.0
}

/** Which UI drill a rung uses — also what review history records. */
fun reviewModeFor(stage: ReviewStage): ReviewMode = when (stage) {
    ReviewStage.READ -> ReviewMode.FLASHCARD
    ReviewStage.CLOZE_30, ReviewStage.CLOZE_60, ReviewStage.CLOZE_90 -> ReviewMode.FILL_IN_BLANK
    ReviewStage.FIRST_LETTERS -> ReviewMode.FIRST_LETTERS
    ReviewStage.REVERSE -> ReviewMode.REVERSE
    ReviewStage.FULL_RECALL -> ReviewMode.TYPE_WHOLE
}

/**
 * Place a pre-ladder verse on the ladder. The old rotation drilled by
 * repetitions % 5, so a verse's successful-review count is a fair record
 * of how far up the ladder it had climbed. Called once, at load, when
 * `stage` is null; every verse graded after that carries a real stage.
 */
fun ReviewProgress.migrated(): ReviewProgress {
    if (stage != null) return this
    val rung = when {
        repetitions <= 0 -> ReviewStage.READ
        repetitions == 1 -> ReviewStage.CLOZE_30
        repetitions == 2 -> ReviewStage.CLOZE_60
        repetitions == 3 -> ReviewStage.CLOZE_90
        repetitions == 4 -> ReviewStage.FIRST_LETTERS
        else -> ReviewStage.FULL_RECALL
    }
    return copy(stage = rung)
}

/** The verse as its initials, e.g. "For God loved" → "F G l". */
fun firstLetters(text: String): String =
    text.trim().split(Regex("\\s+")).joinToString(" ") { it.take(1) }

/**
 * Word-by-word verdict on a typed verse. Tokens are the expected verse in
 * order; [statuses] says what the typist did with each word. A near-miss
 * (edit distance 1 on a substantial word — "beloved" for "beloveth") is
 * worth half a word, so a typo'd recitation still reads as knowing it.
 */
data class TypedVerseCheck(
    val tokens: List<String>,
    val statuses: List<WordStatus>,
    /** 1.0 per correct word, 0.5 per near-miss, 0 per miss — of the whole. */
    val accuracy: Double,
)

enum class WordStatus { CORRECT, NEAR, WRONG }

fun checkTypedVerse(expected: String, typed: String): TypedVerseCheck {
    val expectedWords = expected.trim().split(Regex("\\s+"))
    val typedWords = typed.trim().split(Regex("\\s+"))

    val statuses = expectedWords.mapIndexed { i, word ->
        if (i >= typedWords.size) {
            WordStatus.WRONG
        } else {
            val want = normalizeWord(word)
            val got = normalizeWord(typedWords[i])
            when {
                got == want -> WordStatus.CORRECT
                isNearMiss(want, got) -> WordStatus.NEAR
                else -> WordStatus.WRONG
            }
        }
    }
    val score = statuses.sumOf {
        when (it) {
            WordStatus.CORRECT -> 1.0
            WordStatus.NEAR -> 0.5
            WordStatus.WRONG -> 0.0
        }
    }
    return TypedVerseCheck(expectedWords, statuses, score / expectedWords.size)
}

/**
 * Cloze verdict: the blanked words in verse order, checked as a typed
 * sequence. Accuracy covers only the words that were blanked — the words
 * still on the page were never tested.
 */
fun checkCloze(puzzle: FillInBlank, typed: String): TypedVerseCheck =
    checkTypedVerse(puzzle.blankedIndexes.joinToString(" ") { puzzle.tokens[it] }, typed)

/**
 * What the measured accuracy suggests grading. The user can still tap a
 * different button — this only pre-highlights the honest choice.
 */
fun suggestGrade(accuracy: Double): Grade = when {
    accuracy >= 0.99 -> Grade.EASY
    accuracy >= 0.80 -> Grade.GOOD
    accuracy >= 0.50 -> Grade.HARD
    else -> Grade.AGAIN
}

/**
 * One edit apart, Damerau-style — a substitution, insertion, deletion, or an
 * adjacent transposition ("belvoed" for "beloved"), by bounded scan. Only
 * substantial words qualify: a short word's near-miss is just a miss.
 */
private fun isNearMiss(want: String, got: String): Boolean {
    if (want.length < 4) return false
    if (want.length - got.length !in -1..1) return false
    var i = 0
    var j = 0
    var edits = 0
    while (i < want.length && j < got.length) {
        if (want[i] == got[j]) {
            i++
            j++
            continue
        }
        if (++edits > 1) return false
        when {
            // Adjacent transposition — the classic fat-finger swap.
            i + 1 < want.length && j + 1 < got.length &&
                want[i] == got[j + 1] && want[i + 1] == got[j] -> { i += 2; j += 2 }
            want.length > got.length -> i++
            want.length < got.length -> j++
            else -> { i++; j++ } // substitution consumes both
        }
    }
    // Whatever trails the side that ran out first counts as the last edit.
    return edits + (want.length - i) + (got.length - j) <= 1
}

/** Compare a word leniently: case and surrounding punctuation don't matter. */
fun normalizeWord(word: String): String = word
    .trim()
    .lowercase()
    .trim('.', ',', ';', ':', '!', '?', '"', '“', '”', '‘', '’')