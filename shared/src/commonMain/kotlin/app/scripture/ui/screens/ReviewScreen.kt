package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import app.scripture.ScriptureStore
import app.scripture.domain.FillInBlank
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.Verse
import app.scripture.domain.makeFillInBlank
import app.scripture.domain.nextProgress
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

/** A review session: flashcards and fill-in-the-blank, graded with SRS buttons. */
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
            // First encounter of a verse is always a plain flashcard;
            // after that, alternate flashcard and fill-in-the-blank.
            val puzzle = remember(current.id, current.progress.repetitions) {
                val seed = current.id.hashCode().toLong() xor current.progress.repetitions.toLong()
                makeFillInBlank(current.text, seed)
            }
            val useFillInBlank = current.progress.repetitions % 2 == 1 && puzzle.blankedIndexes.isNotEmpty()

            if (useFillInBlank) {
                FillInBlankReview(store = store, verse = current, puzzle = puzzle)
            } else {
                FlashcardReview(store = store, verse = current)
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

/** Show the reference, recall from memory, reveal, then grade. */
@Composable
private fun FlashcardReview(store: ScriptureStore, verse: Verse) {
    var revealed by remember(verse.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = "${verse.reference.uppercase()} — recall it first",
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
                text = if (revealed) verse.text else "Tap to reveal",
                style = if (revealed) MaterialTheme.scriptureBody else MaterialTheme.scriptureBody,
                color = if (revealed) MaterialTheme.scriptureColor else MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (revealed) {
            GradeBar(
                preview = { grade -> intervalPreview(verse, grade, store.todayEpochDay()) },
                onGrade = { grade -> store.gradeCurrent(grade, ReviewMode.FLASHCARD) },
            )
        } else {
            PrimaryButton(
                text = "Show verse",
                onClick = { revealed = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Type the missing words, check, then grade honestly. */
@Composable
private fun FillInBlankReview(store: ScriptureStore, verse: Verse, puzzle: FillInBlank) {
    val blanks = puzzle.blankedIndexes
    var answers by remember(verse.id, verse.progress.repetitions) {
        mutableStateOf(List(blanks.size) { "" })
    }
    var checked by remember(verse.id, verse.progress.repetitions) { mutableStateOf(false) }

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
            val isCorrect = checked && normalize(answers[blankIndex]) == normalize(actualWord)

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
            GradeBar(
                preview = { grade -> intervalPreview(verse, grade, store.todayEpochDay()) },
                onGrade = { grade -> store.gradeCurrent(grade, ReviewMode.FILL_IN_BLANK) },
            )
        }
    }
}

/** "now", "1d", "6d"… — what choosing this grade would mean. */
private fun intervalPreview(verse: Verse, grade: Grade, today: Long): String {
    val next = nextProgress(verse.progress, grade, today)
    val days = (next.dueEpochDay ?: today) - today
    return if (days == 0L) "now" else "${days}d"
}

/** Compare typed answers leniently: case and surrounding punctuation don't matter. */
private fun normalize(word: String): String = word
    .trim()
    .lowercase()
    .trim('.', ',', ';', ':', '!', '?', '"', '“', '”', '‘', '’')