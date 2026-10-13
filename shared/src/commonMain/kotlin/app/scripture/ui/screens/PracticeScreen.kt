package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.scripture.ScriptureStore
import app.scripture.domain.ReviewMode
import app.scripture.domain.ReviewStage
import app.scripture.domain.Verse
import app.scripture.domain.clozeFraction
import app.scripture.domain.makeFillInBlank
import app.scripture.domain.reviewModeFor
import house.design.ChoiceRow
import house.design.EmptyState
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing
import app.scripture.ui.components.drillLabel

/**
 * One verse, one drill, on demand — chosen from the verse's detail screen.
 * Pure exercise: nothing is graded, scheduled, or written to history, which
 * is the whole point — practice never disturbs the spaced-repetition plan.
 * "Practice again" reruns the drill with a fresh round number, so cloze
 * reshuffles its blanks.
 */
@Composable
fun PracticeScreen(
    store: ScriptureStore,
    verseId: String,
    mode: ReviewMode,
    onExit: () -> Unit,
) {
    val verses by store.verses.collectAsState()
    val verse = verses.firstOrNull { it.id == verseId }
    var round by remember(verseId) { mutableIntStateOf(0) }

    ScreenScaffold(title = "Practice", onBack = onExit) {
        if (verse == null) {
            EmptyState("Verse not found", "It may have been deleted.")
            return@ScreenScaffold
        }

        val footer: @Composable (app.scripture.domain.Grade?) -> Unit = {
            PracticeFooter(onAgain = { round++ }, onDone = onExit)
        }

        // Keyed by round: "Practice again" restarts the drill with fresh
        // reveal/answer state and a newly shuffled puzzle.
        key(round) {
            when (mode) {
                ReviewMode.FLASHCARD -> RecallReview(verse = verse, answerFooter = footer)

                ReviewMode.FILL_IN_BLANK -> {
                    // Practice at the verse's own rung when it's on a cloze
                    // rung; otherwise a mid-ladder workout.
                    val stage = verse.progress.stage ?: ReviewStage.READ
                    val fraction = clozeFraction(stage).let { if (it > 0.0) it else 0.6 }
                    val puzzle = remember(verse.id, round) {
                        makeFillInBlank(
                            verse.text,
                            seed = verse.id.hashCode().toLong() xor round.toLong(),
                            fraction = fraction,
                        )
                    }
                    // A verse too short to blank out falls back to a reveal.
                    if (puzzle.blankedIndexes.isEmpty()) {
                        RecallReview(verse = verse, answerFooter = footer)
                    } else {
                        ClozeReview(verse = verse, puzzle = puzzle, answerFooter = footer)
                    }
                }

                ReviewMode.FIRST_LETTERS -> FirstLettersTypedReview(verse = verse, answerFooter = footer)
                ReviewMode.REVERSE -> ReverseReview(verse = verse, answerFooter = footer)
                ReviewMode.TYPE_WHOLE -> TypedVerseReview(verse = verse, answerFooter = footer)
            }
        }
    }
}

/** Practice's answer footer: run it again, or be done. No grades here —
 *  the suggested grade a drill computes is practice's to ignore. */
@Composable
private fun PracticeFooter(onAgain: () -> Unit, onDone: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
    ) {
        PrimaryButton("Practice again", onClick = onAgain, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

/**
 * The verse detail screen's practice picker: a radio row per drill with the
 * verse's current ladder drill preselected, and the button that launches it.
 */
@Composable
fun PracticePicker(verse: Verse, onPractice: (ReviewMode) -> Unit) {
    var chosen by remember(verse.id) {
        mutableStateOf(reviewModeFor(verse.progress.stage ?: ReviewStage.READ))
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        ReviewMode.entries.forEach { mode ->
            ChoiceRow(
                label = drillLabel.getValue(mode),
                selected = chosen == mode,
                onClick = { chosen = mode },
            )
        }
        PrimaryButton(
            text = "Practice",
            onClick = { onPractice(chosen) },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}