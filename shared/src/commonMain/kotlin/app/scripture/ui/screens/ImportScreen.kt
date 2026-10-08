package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import app.scripture.ScriptureStore
import app.scripture.domain.ImportOutcome
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing
import app.scripture.ui.designsystem.body
import app.scripture.ui.designsystem.label
import kotlinx.coroutines.launch

/**
 * Bulk-add verses from JSON. Paste text or pick a file — the file lands in
 * the same field so its contents are visible before committing. Either way
 * one Import button runs the same forgiving path and reports counts.
 */
@Composable
fun ImportScreen(
    store: ScriptureStore,
    onDone: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var outcome by remember { mutableStateOf<ImportOutcome?>(null) }
    var pickError by remember { mutableStateOf(false) }
    val picker = rememberImportFilePicker()
    val scope = rememberCoroutineScope()

    ScreenScaffold(title = "Import verses", onBack = onDone) {
        val result = outcome
        if (result == null) {
            Text(
                "Paste verses as JSON, or choose a .json file. " +
                    "Verses already in your library are skipped.",
                style = MaterialTheme.body,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
            Text(
                formatExample,
                style = MaterialTheme.label.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(bottom = Spacing.md),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = {
                    pickError = false
                    scope.launch {
                        when (val picked = picker.pick()) {
                            is PickOutcome.Read -> input = picked.text
                            PickOutcome.Unreadable -> pickError = true
                            PickOutcome.Cancelled -> {}
                        }
                    }
                }) { Text("Choose a file…") }
                if (pickError) {
                    Text(
                        "Couldn't read that file.",
                        style = MaterialTheme.label,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    pickError = false
                },
                label = { Text("Paste JSON") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.md),
            )
            PrimaryButton(
                text = "Import",
                onClick = { outcome = store.importVerses(input) },
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                outcomeMessage(result).forEach { line ->
                    Text(line, style = MaterialTheme.body)
                }
            }
            PrimaryButton(
                text = "Done",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            )
            OutlinedButton(
                onClick = {
                    input = ""
                    outcome = null
                },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            ) { Text("Import more") }
        }
    }
}

private const val formatExample =
    """[{"reference": "John 3:16", "text": "…", "tags": ["love"], "translation": "NWT"}]"""

/** Counts become plain sentences — no jargon like "rows" or "records". */
private fun outcomeMessage(outcome: ImportOutcome): List<String> = when (outcome) {
    ImportOutcome.NotJson -> listOf("That doesn't look like JSON. Check that the text starts with [ or { and try again.")
    is ImportOutcome.Imported -> buildList {
        when (outcome.addedCount) {
            0 -> add("No new verses — everything in that batch was already in your library.")
            1 -> add("Imported 1 verse.")
            else -> add("Imported ${outcome.addedCount} verses.")
        }
        if (outcome.duplicateCount > 0) {
            add("Skipped ${outcome.duplicateCount} ${if (outcome.duplicateCount == 1) "verse" else "verses"} already in your library.")
        }
        if (outcome.invalidCount > 0) {
            add("Skipped ${outcome.invalidCount} incomplete ${if (outcome.invalidCount == 1) "entry" else "entries"} (missing reference or text).")
        }
    }
}