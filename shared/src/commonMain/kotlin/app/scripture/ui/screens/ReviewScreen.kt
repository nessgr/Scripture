package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import app.scripture.ScriptureStore
import app.scripture.domain.FillInBlank
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.Verse
import app.scripture.domain.checkTypedVerse
import app.scripture.domain.firstLetters
import app.scripture.domain.makeFillInBlank
import app.scripture.domain.maxBlanksFor
import app.scripture.domain.nextProgress
import app.scripture.domain.normalizeWord
import app.scripture.domain.reviewModeFor
import app.scripture.ui.designsystem.ClickableCard
import app.scripture.ui.designsystem.EmptyState
import app.scripture.ui.designsystem.GradeBar
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing
import app.scripture.ui.designsystem.label
import app.scripture.ui.designsystem.scriptureBody
import app.scripture.ui.designsystem.scriptureColor
import app.scripture.ui.designsystem.scriptureDisplay

/**
 * A review session: five drills rotate by how well the verse is already
 * known (see domain/Drills.kt), each graded with the SRS buttons.
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
            // Which drill this review gets: the rotation asks for more of
            // the verse each round the user remembers it.
            when (reviewModeFor(current.progress.repetitions)) {
                ReviewMode.FLASHCARD -> RecallReview(
                    verse = current,
                    hint = "recall it first",
                    hidden = "Tap to reveal",
                ) { GradeFooter(store, current, ReviewMode.FLASHCARD) }
                ReviewMode.FILL_IN_BLANK -> {
                    val puzzle = remember(current.id, current.progress.repetitions) {
                        makeFillInBlank(
                            current.text,
                            seed = current.id.hashCode().toLong() xor current.progress.repetitions.toLong(),
                            maxBlanks = maxBlanksFor(current.progress.repetitions),
                        )
                    }
                    // A verse too short to blank out falls back to a reveal.
                    if (puzzle.blankedIndexes.isEmpty()) {
                        RecallReview(
                            verse = current,
                            hint = "recall it first",
                            hidden = "Tap to reveal",
                        ) { GradeFooter(store, current, ReviewMode.FLASHCARD) }
                    } else {
                        FillInBlankReview(verse = current, puzzle = puzzle) {
                            GradeFooter(store, current, ReviewMode.FILL_IN_BLANK)
                        }
                    }
                }
                ReviewMode.FIRST_LETTERS -> RecallReview(
                    verse = current,
                    hint = "say it from the first letters",
                    hidden = firstLetters(current.text),
                ) { GradeFooter(store, current, ReviewMode.FIRST_LETTERS) }
                ReviewMode.REVERSE -> ReverseReview(verse = current) {
                    GradeFooter(store, current, ReviewMode.REVERSE)
                }
                ReviewMode.TYPE_WHOLE -> TypedVerseReview(verse = current) {
                    GradeFooter(store, current, ReviewMode.TYPE_WHOLE)
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

/** The SRS grade bar — review's footer of choice: grading reschedules the verse. */
@Composable
private fun GradeFooter(store: ScriptureStore, verse: Verse, mode: ReviewMode) {
    GradeBar(
        preview = { grade -> intervalPreview(verse, grade, store.todayEpochDay()) },
        onGrade = { grade -> store.gradeCurrent(grade, mode) },
    )
}

/**
 * The reveal drill: reference up top, [hidden] standing in for the verse
 * ("Tap to reveal", or the first letters), then the full text. Serves both
 * the flashcard and the first-letters drill — they differ only in the prompt.
 * The [answerFooter] slot is what shows once the answer is out; review puts
 * the grade bar there, practice puts its own buttons.
 */
@Composable
internal fun RecallReview(
    verse: Verse,
    hint: String,
    hidden: String,
    answerFooter: @Composable () -> Unit,
) {
    var revealed by remember(verse.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = "${verse.reference.uppercase()} — $hint",
            style = MaterialTheme.label,
            color = MaterialTheme.colorScheme.outline,
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
                text = if (revealed) verse.text else hidden,
                style = MaterialTheme.scriptureBody,
                color = if (revealed) MaterialTheme.scriptureColor else MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (revealed) answerFooter() else PrimaryButton(
            text = "Show verse",
            onClick = { revealed = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The verse text is shown — recall WHERE it's from. Mirrors the flashcard,
 * with text and reference trading places.
 */
@Composable
internal fun ReverseReview(
    verse: Verse,
    answerFooter: @Composable () -> Unit,
) {
    var revealed by remember(verse.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = "Where is this from?",
            style = MaterialTheme.label,
            color = MaterialTheme.colorScheme.outline,
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

        if (revealed) answerFooter() else PrimaryButton(
            text = "Show reference",
            onClick = { revealed = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Type the whole verse from the reference, then see word-by-word matches. */
@Composable
internal fun TypedVerseReview(
    verse: Verse,
    answerFooter: @Composable () -> Unit,
) {
    var typed by remember(verse.id) { mutableStateOf("") }
    var checked by remember(verse.id) { mutableStateOf(false) }
    val ink = MaterialTheme.scriptureColor
    val miss = MaterialTheme.colorScheme.error

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

            // The expected verse, matched words in ink, misses in red —
            // the diff IS the answer key.
            Text(
                text = buildAnnotatedString {
                    result.tokens.forEachIndexed { i, token ->
                        withStyle(SpanStyle(color = if (result.isCorrect[i]) ink else miss)) {
                            append(if (i == 0) token else " $token")
                        }
                    }
                },
                style = MaterialTheme.scriptureBody,
            )
            Text(
                text = "${result.matchedCount} of ${result.tokens.size} words matched",
                style = MaterialTheme.label,
                color = MaterialTheme.colorScheme.outline,
            )
            answerFooter()
        }
    }
}

/** Type the missing words, check, then grade honestly. */
@Composable
internal fun FillInBlankReview(
    verse: Verse,
    puzzle: FillInBlank,
    answerFooter: @Composable () -> Unit,
) {
    val blanks = puzzle.blankedIndexes
    var answers by remember(verse.id) {
        mutableStateOf(List(blanks.size) { "" })
    }
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

        blanks.forEachIndexed { blankIndex, tokenIndex ->
            val actualWord = puzzle.tokens[tokenIndex]
            val isCorrect = checked && normalizeWord(answers[blankIndex]) == normalizeWord(actualWord)

            OutlinedTextField(
                value = answers[blankIndex],
                onValueChange = { value ->
                    if (!checked) answers = answers.toMutableList().also { it[blankIndex] = value }
                },
                label = { Text("Missing word ${blankIndex + 1}") },
                supportingText = {
                    if (checked) {
                        Text(if (isCorrect) "✓ $actualWord" else "✗ ${puzzle.tokens[tokenIndex]}")
                    }
                },
                isError = checked && !isCorrect,
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrect = false),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!checked) {
            PrimaryButton(
                text = "Check",
                onClick = { checked = true },
                enabled = answers.all { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            answerFooter()
        }
    }
}

/** "now", "1d", "6d"… — what choosing this grade would mean. */
private fun intervalPreview(verse: Verse, grade: Grade, today: Long): String {
    val next = nextProgress(verse.progress, grade, today)
    val days = (next.dueEpochDay ?: today) - today
    return if (days == 0L) "now" else "${days}d"
}