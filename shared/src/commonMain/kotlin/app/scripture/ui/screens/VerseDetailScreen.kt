package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.scripture.ScriptureStore
import app.scripture.ui.designsystem.EmptyState
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing
import app.scripture.ui.designsystem.TagChip
import app.scripture.ui.designsystem.label
import app.scripture.ui.designsystem.reference
import app.scripture.ui.designsystem.scriptureBody
import app.scripture.ui.designsystem.scriptureColor

/**
 * One verse, opened from the library: the full text (the list card truncates
 * to three lines), where its review stands, and the way out — edit or delete.
 */
@Composable
fun VerseDetailScreen(
    store: ScriptureStore,
    verseId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val verses by store.verses.collectAsState()
    val verse = verses.firstOrNull { it.id == verseId }
    var confirmDelete by remember { mutableStateOf(false) }

    if (verse == null) {
        // Unreachable in normal flow — the id always comes from a tapped card —
        // but a stale navigation target must not crash the app.
        ScreenScaffold(title = "Verse", onBack = onBack) {
            EmptyState("Verse not found", "It may have been deleted.")
        }
        return
    }

    ScreenScaffold(title = verse.reference.uppercase(), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text(
                verse.text,
                style = MaterialTheme.scriptureBody,
                color = MaterialTheme.scriptureColor,
            )

            if (verse.translation != null || verse.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (verse.translation != null) Text(
                        verse.translation,
                        style = MaterialTheme.label,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    verse.tags.forEach { TagChip(it) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    masteryDots(verse.progress.repetitions),
                    style = MaterialTheme.label,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    reviewStatus(verse.progress.dueEpochDay, store.todayEpochDay()),
                    style = MaterialTheme.label,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            PrimaryButton(
                text = "Edit verse",
                onClick = { onEdit(verse.id) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
            )
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete verse?") },
            text = {
                Text("“${verse.reference}” and its review history will be removed. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteVerse(verse.id)
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/** ● per two consecutive successes, up to five — a quiet mastery meter. */
private fun masteryDots(repetitions: Int): String =
    "●".repeat((repetitions / 2).coerceAtMost(5)) +
        "○".repeat(5 - (repetitions / 2).coerceAtMost(5))

/** Plain epoch-day arithmetic — no per-platform date formatting needed. */
private fun reviewStatus(dueEpochDay: Long?, today: Long): String = when {
    dueEpochDay == null -> "Never reviewed"
    dueEpochDay <= today -> "Due today"
    else -> "Next review in ${dueEpochDay - today} days"
}