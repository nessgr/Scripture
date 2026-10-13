package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.scripture.ScriptureStore
import app.scripture.domain.FillInBlank
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.ReviewStage
import app.scripture.domain.TypedVerseCheck
import app.scripture.domain.Verse
import app.scripture.domain.WordStatus
import app.scripture.domain.checkCloze
import app.scripture.domain.checkTypedVerse
import app.scripture.domain.clozeFraction
import app.scripture.domain.firstLetters
import app.scripture.domain.makeFillInBlank
import app.scripture.domain.nextProgress
import app.scripture.domain.suggestGrade
import app.scripture.ui.designsystem.ClickableCard
import app.scripture.ui.designsystem.EmptyState
import app.scripture.ui.designsystem.GradeBar
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing
import app.scripture.ui.designsystem.body
import app.scripture.ui.designsystem.gradeLabel
import app.scripture.ui.designsystem.hintColor
import app.scripture.ui.designsystem.label
import app.scripture.ui.designsystem.nearMissColor
import app.scripture.ui.designsystem.scriptureBody
import app.scripture.ui.designsystem.scriptureColor
import app.scripture.ui.designsystem.scriptureDisplay

/**
 * A review session: each verse drills at its rung on the mastery ladder
 * (see domain/Drills.kt) — the better it's known, the more of it the drill
 * asks for. Typed drills measure accuracy and suggest the honest grade;
 * reveal drills ask for an attempt before they show the answer.
 */
@Composable
fun ReviewScreen(
    store: ScriptureStore,
    onExit: () -> Unit,
) {
    val queue by store.reviewQueue.collectAsState()
    val current = queue.firstOrNull()

    ScreenScaffold(
        title = "Review",
        onBack = {
            store.endReview()
            onExit()
        },
    ) {
        if (current == null) {
            SessionSummary(onExit = onExit)
        } else {
            // Which drill this review gets: the verse's rung on the ladder.
            // Null cannot occur after load's migration, but the fallback keeps
            // a never-loaded verse from crashing a review.
            val stage = current.progress.stage ?: ReviewStage.READ
            when (stage) {
                ReviewStage.READ -> RecallReview(verse = current) { suggested ->
                    GradeFooter(store, current, ReviewMode.FLASHCARD, suggested)
                }

                ReviewStage.CLOZE_30, ReviewStage.CLOZE_60, ReviewStage.CLOZE_90 -> {
                    val puzzle = remember(current.id, current.progress.repetitions) {
                        makeFillInBlank(
                            current.text,
                            seed = current.id.hashCode().toLong() xor current.progress.repetitions.toLong(),
                            fraction = clozeFraction(stage),
                        )
                    }
                    // A verse too short to blank out falls back to a reveal.
                    if (puzzle.blankedIndexes.isEmpty()) {
                        RecallReview(verse = current) { suggested ->
                            GradeFooter(store, current, ReviewMode.FLASHCARD, suggested)
                        }
                    } else {
                        ClozeReview(verse = current, puzzle = puzzle) { suggested ->
                            GradeFooter(store, current, ReviewMode.FILL_IN_BLANK, suggested)
                        }
                    }
                }

                ReviewStage.FIRST_LETTERS -> FirstLettersTypedReview(verse = current) { suggested ->
                    GradeFooter(store, current, ReviewMode.FIRST_LETTERS, suggested)
                }

                ReviewStage.REVERSE -> ReverseReview(verse = current) { suggested ->
                    GradeFooter(store, current, ReviewMode.REVERSE, suggested)
                }

                ReviewStage.FULL_RECALL -> TypedVerseReview(verse = current) { suggested ->
                    GradeFooter(store, current, ReviewMode.TYPE_WHOLE, suggested)
                }
            }
        }
    }
}

@Composable
private fun SessionSummary(onExit: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        EmptyState(
            message = "Session complete",
            hint = "Come back tomorrow — that's when spaced repetition works its magic.",
        )
        PrimaryButton("Back to library", onClick = onExit, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * The SRS grade bar — review's footer of choice: grading reschedules the
 * verse and moves it along the ladder. [suggested] highlights the grade the
 * drill itself measured or the attempt the user reported.
 */
@Composable
private fun GradeFooter(
    store: ScriptureStore,
    verse: Verse,
    mode: ReviewMode,
    suggested: Grade?,
) {
    GradeBar(
        preview = { grade -> intervalPreview(verse, grade, store.todayEpochDay()) },
        onGrade = { grade -> store.gradeCurrent(grade, mode) },
        suggested = suggested,
    )
}

/**
 * The reveal drill at the bottom of the ladder: reference shown, recall the
 * text — and the gate makes the attempt explicit. "I said it" and "Couldn't
 * recall" both reveal the verse; they differ only in the grade they suggest,
 * because effortful retrieval — not re-reading — is what builds memory.
 * Tapping the card itself reveals without a reported attempt.
 */
@Composable
internal fun RecallReview(
    verse: Verse,
    answerFooter: @Composable (Grade?) -> Unit,
) {
    var revealed by remember(verse.id) { mutableStateOf(false) }
    var suggested by remember(verse.id) { mutableStateOf<Grade?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = "${verse.reference.uppercase()} — recall it first",
            style = MaterialTheme.label,
            color = MaterialTheme.hintColor,
        )

        ClickableCard(onClick = { revealed = true }) {
            Text(
                text = verse.reference.uppercase(),
                style = MaterialTheme.scriptureDisplay,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = if (revealed) verse.text else "Tap to reveal",
                style = MaterialTheme.scriptureBody,
                color = if (revealed) MaterialTheme.scriptureColor else MaterialTheme.hintColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (revealed) {
            answerFooter(suggested)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(
                    text = "I said it",
                    onClick = { suggested = Grade.GOOD; revealed = true },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { suggested = Grade.AGAIN; revealed = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Couldn't recall", style = MaterialTheme.body) }
            }
        }
    }
}

/**
 * The verse text is shown — recall WHERE it's from. Same retrieval gate as
 * the flashcard, with the report button wording turned around.
 */
@Composable
internal fun ReverseReview(
    verse: Verse,
    answerFooter: @Composable (Grade?) -> Unit,
) {
    var revealed by remember(verse.id) { mutableStateOf(false) }
    var suggested by remember(verse.id) { mutableStateOf<Grade?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = "Where is this from?",
            style = MaterialTheme.label,
            color = MaterialTheme.hintColor,
        )

        ClickableCard(onClick = { revealed = true }) {
            Text(
                text = verse.text,
                style = MaterialTheme.scriptureBody,
                color = MaterialTheme.scriptureColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (revealed) {
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = verse.reference.uppercase(),
                    style = MaterialTheme.scriptureDisplay,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (revealed) {
            answerFooter(suggested)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(
                    text = "I knew it",
                    onClick = { suggested = Grade.GOOD; revealed = true },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { suggested = Grade.AGAIN; revealed = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Didn't know", style = MaterialTheme.body) }
            }
        }
    }
}

/** Type the verse from the reference alone — the top of the ladder. */
@Composable
internal fun TypedVerseReview(
    verse: Verse,
    answerFooter: @Composable (Grade?) -> Unit,
) {
    var typed by remember(verse.id) { mutableStateOf("") }
    var checked by remember(verse.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(
            text = verse.reference.uppercase(),
            style = MaterialTheme.scriptureDisplay,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!checked) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = { Text("Type the verse from memory") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(autoCorrect = false),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Check",
                onClick = { checked = true },
                enabled = typed.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val result = checkTypedVerse(verse.text, typed)
            TypedDiff(result.tokens, result.statuses)
            DiffSummary(result)
            answerFooter(suggestGrade(result.accuracy))
        }
    }
}

/**
 * The first-letters drill: the verse as initials, then typed in full —
 * the bridge between cloze and free recall. The initials cue the memory;
 * typing the whole verse is still the retrieval effort.
 */
@Composable
internal fun FirstLettersTypedReview(
    verse: Verse,
    answerFooter: @Composable (Grade?) -> Unit,
) {
    var typed by remember(verse.id) { mutableStateOf("") }
    var checked by remember(verse.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(
            text = verse.reference.uppercase(),
            style = MaterialTheme.scriptureDisplay,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // The cue: initials only, quiet enough to lean on, too sparse to read.
        Text(
            text = firstLetters(verse.text),
            style = MaterialTheme.scriptureBody,
            color = MaterialTheme.hintColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!checked) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = { Text("Say it back — type the verse") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(autoCorrect = false),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Check",
                onClick = { checked = true },
                enabled = typed.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val result = checkTypedVerse(verse.text, typed)
            TypedDiff(result.tokens, result.statuses)
            DiffSummary(result)
            answerFooter(suggestGrade(result.accuracy))
        }
    }
}

/**
 * Cloze: the verse with a fraction of its words blanked, typed back in
 * order. One input for however many blanks — a 90% blank is the same
 * gesture as a 30% blank, and both grow toward typing the whole verse.
 */
@Composable
internal fun ClozeReview(
    verse: Verse,
    puzzle: FillInBlank,
    answerFooter: @Composable (Grade?) -> Unit,
) {
    val blanks = puzzle.blankedIndexes
    var typed by remember(verse.id) { mutableStateOf("") }
    var checked by remember(verse.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(
            text = verse.reference.uppercase(),
            style = MaterialTheme.scriptureDisplay,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // The verse with blanks rendered as sized underscores.
        Text(
            text = puzzle.tokens.mapIndexed { i, token ->
                if (i in blanks) "_".repeat(token.length) else token
            }.joinToString(" "),
            style = MaterialTheme.scriptureBody,
            color = MaterialTheme.scriptureColor,
        )

        if (!checked) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = { Text("Type the missing words, in order") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(autoCorrect = false),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Check",
                onClick = { checked = true },
                enabled = typed.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val result = checkCloze(puzzle, typed)

            // The expected verse with the blanks filled back in, colored by
            // the verdict — the diff IS the answer key. Words that stayed
            // on the page were never tested, so they read as plain ink.
            val blankStatuses = blanks.withIndex().associate { (k, index) -> index to result.statuses[k] }
            TypedDiff(
                tokens = puzzle.tokens,
                statuses = puzzle.tokens.indices.map { blankStatuses[it] ?: WordStatus.CORRECT },
            )
            DiffSummary(result)
            answerFooter(suggestGrade(result.accuracy))
        }
    }
}

/** The expected verse, each word colored by what the typist did with it. */
@Composable
internal fun TypedDiff(tokens: List<String>, statuses: List<WordStatus>) {
    val ink = MaterialTheme.scriptureColor
    val amber = MaterialTheme.nearMissColor
    val miss = MaterialTheme.colorScheme.error
    Text(
        text = buildAnnotatedString {
            tokens.forEachIndexed { i, token ->
                val color = when (statuses[i]) {
                    WordStatus.CORRECT -> ink
                    WordStatus.NEAR -> amber
                    WordStatus.WRONG -> miss
                }
                withStyle(SpanStyle(color = color)) {
                    append(if (i == 0) token else " $token")
                }
            }
        },
        style = MaterialTheme.scriptureBody,
    )
}

/** "17 of 20 words right, 1 near-miss · suggested: Good" — the score line. */
@Composable
private fun DiffSummary(result: TypedVerseCheck) {
    val right = result.statuses.count { it == WordStatus.CORRECT }
    val near = result.statuses.count { it == WordStatus.NEAR }
    val suggested = suggestGrade(result.accuracy)
    val text = buildString {
        append("$right of ${result.tokens.size} words right")
        if (near > 0) {
            append(if (near == 1) ", 1 near-miss" else ", $near near-misses")
        }
        append(" · suggested: ${gradeLabel.getValue(suggested)}")
    }
    Text(
        text = text,
        style = MaterialTheme.label,
        color = MaterialTheme.hintColor,
    )
}

/** "now", "1d", "6d"… — what choosing this grade would mean. */
private fun intervalPreview(verse: Verse, grade: Grade, today: Long): String {
    val next = nextProgress(verse.progress, grade, today)
    val days = (next.dueEpochDay ?: today) - today
    return if (days == 0L) "now" else "${days}d"
}