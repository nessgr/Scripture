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
import app.scripture.domain.ImportMode
import app.scripture.domain.ImportOutcome
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing
import house.design.body
import house.design.hintColor
import house.design.label
import house.design.title
import kotlinx.coroutines.launch

/**
 * Bulk-add verses from JSON. Paste text or pick a file — the file lands in
 * the same field so its contents are visible before committing. Import
 * first shows a summary of what the JSON holds (and its title, if any),
 * then asks the one question a commit has to answer: add to the library,
 * or replace it?
 */
@Composable
fun ImportScreen(
    store: ScriptureStore,
    onDone: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<ImportOutcome.Imported?>(null) }
    var outcome by remember { mutableStateOf<ImportOutcome?>(null) }
    var committedMode by remember { mutableStateOf(ImportMode.ADD) }
    var pickError by remember { mutableStateOf(false) }
    var notJson by remember { mutableStateOf(false) }
    val picker = rememberImportFilePicker()
    val scope = rememberCoroutineScope()

    fun commit(mode: ImportMode) {
        committedMode = mode
        outcome = store.importVerses(input, mode)
    }

    // Tall screen: intro + example + picker + 6-line field push the action
    // buttons past the window edge without this — they'd be unreachable,
    // so an import silently "does nothing."
    ScreenScaffold(title = "Import verses", onBack = onDone, scrollable = true) {
        val result = outcome
        val ready = preview
        when {
            result != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    outcomeMessage(result, committedMode).forEach { line ->
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
            ready != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    previewLines(ready).forEach { line ->
                        Text(line, style = MaterialTheme.body)
                    }
                }
                Text(
                    "How should this apply?",
                    style = MaterialTheme.title,
                    modifier = Modifier.padding(top = Spacing.lg),
                )
                PrimaryButton(
                    text = "Add to my library",
                    onClick = { commit(ImportMode.ADD) },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )
                OutlinedButton(
                    // A batch with no complete verses can add nothing, and
                    // replacing with it would just empty the library.
                    enabled = ready.addedCount > 0,
                    onClick = { commit(ImportMode.REPLACE) },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                ) { Text("Replace library") }
                Text(
                    "Replacing removes every verse currently in your library.",
                    style = MaterialTheme.label,
                    color = MaterialTheme.hintColor,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                OutlinedButton(
                    onClick = { preview = null },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                ) { Text("Edit the JSON") }
            }
            else -> {
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
                            color = MaterialTheme.hintColor,
                            modifier = Modifier.padding(top = Spacing.sm),
                        )
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        pickError = false
                        notJson = false
                    },
                    label = { Text("Paste JSON") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.md),
                )
                if (notJson) {
                    Text(
                        "That doesn't look like JSON. Check that the text starts with [ or { and try again.",
                        style = MaterialTheme.body,
                        color = MaterialTheme.hintColor,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                }
                PrimaryButton(
                    text = "Import",
                    onClick = {
                        notJson = false
                        when (val p = store.previewImport(input)) {
                            ImportOutcome.NotJson -> notJson = true
                            is ImportOutcome.Imported -> preview = p
                        }
                    },
                    enabled = input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val formatExample =
    """{"title": "My verses", "verses": [{"reference": "John 3:16", "text": "…", "tags": ["love"]}]}"""

/** Preview counts become plain sentences — no jargon like "rows" or "records". */
private fun previewLines(ready: ImportOutcome.Imported): List<String> = buildList {
    add(if (ready.title != null) "Ready to import “${ready.title}”." else "Ready to import:")
    when (ready.addedCount) {
        0 -> add("No complete verses were found in that JSON.")
        1 -> add("1 verse is ready.")
        else -> add("${ready.addedCount} verses are ready.")
    }
    if (ready.duplicateCount > 0) {
        add("Skipped ${ready.duplicateCount} ${if (ready.duplicateCount == 1) "verse" else "verses"} repeated in the file.")
    }
    if (ready.invalidCount > 0) {
        add("Skipped ${ready.invalidCount} incomplete ${if (ready.invalidCount == 1) "entry" else "entries"} (missing reference or text).")
    }
}

/** Counts become plain sentences — no jargon like "rows" or "records". */
private fun outcomeMessage(outcome: ImportOutcome, mode: ImportMode): List<String> = when (outcome) {
    ImportOutcome.NotJson -> listOf("That doesn't look like JSON. Check that the text starts with [ or { and try again.")
    is ImportOutcome.Imported -> buildList {
        when (mode) {
            ImportMode.ADD -> when (outcome.addedCount) {
                0 -> add("No new verses — everything in that batch was already in your library.")
                1 -> add("Imported 1 verse.")
                else -> add("Imported ${outcome.addedCount} verses.")
            }
            ImportMode.REPLACE -> when (outcome.addedCount) {
                0 -> add("Your library is now empty.")
                else -> add(
                    if (outcome.title != null) "Your library now holds the ${outcome.addedCount} verses of “${outcome.title}”."
                    else "Your library now holds ${outcome.addedCount} verses — everything else was removed.",
                )
            }
        }
        if (outcome.duplicateCount > 0) {
            add(
                if (mode == ImportMode.ADD) {
                    "Skipped ${outcome.duplicateCount} ${if (outcome.duplicateCount == 1) "verse" else "verses"} already in your library."
                } else {
                    "Skipped ${outcome.duplicateCount} ${if (outcome.duplicateCount == 1) "verse" else "verses"} repeated in the file."
                },
            )
        }
        if (outcome.invalidCount > 0) {
            add("Skipped ${outcome.invalidCount} incomplete ${if (outcome.invalidCount == 1) "entry" else "entries"} (missing reference or text).")
        }
        if (mode == ImportMode.ADD && outcome.title != null) {
            add("Filed under “${outcome.title}”.")
        }
    }
}