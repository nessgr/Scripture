package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.scripture.ui.designsystem.Spacing

/**
 * Field state for adding or editing a verse, pre-fillable from an existing
 * one. A plain @Stable state holder so the form composable stays a dumb
 * renderer with no store knowledge — screens own their save buttons.
 */
@Stable
class VerseFormState(
    initialReference: String = "",
    initialText: String = "",
    initialTags: String = "",
    initialTranslation: String = "",
) {
    var reference by mutableStateOf(initialReference)
    var text by mutableStateOf(initialText)
    var tags by mutableStateOf(initialTags)
    var translation by mutableStateOf(initialTranslation)

    val canSave: Boolean get() = reference.isNotBlank() && text.isNotBlank()

    /** Comma-separated input, e.g. "love, faith" → ["love", "faith"]. */
    val tagList: List<String>
        get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}

/** The four shared fields of the add/edit form. */
@Composable
fun VerseForm(state: VerseFormState, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = modifier) {
        OutlinedTextField(
            value = state.reference,
            onValueChange = { state.reference = it },
            label = { Text("Reference") },
            placeholder = { Text("John 3:16") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.text,
            onValueChange = { state.text = it },
            label = { Text("Verse text") },
            placeholder = { Text("Type or paste the verse…") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.tags,
            onValueChange = { state.tags = it },
            label = { Text("Tags") },
            placeholder = { Text("love, faith — comma-separated") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.translation,
            onValueChange = { state.translation = it },
            label = { Text("Translation (optional)") },
            placeholder = { Text("NWT") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}