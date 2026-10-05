package app.scripture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.scripture.ScriptureStore
import app.scripture.ui.designsystem.PrimaryButton
import app.scripture.ui.designsystem.ScreenScaffold
import app.scripture.ui.designsystem.Spacing

/** Form for adding one verse. Everything is optional except reference + text. */
@Composable
fun AddVerseScreen(
    store: ScriptureStore,
    onDone: () -> Unit,
) {
    var reference by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }

    val canSave = reference.isNotBlank() && text.isNotBlank()

    ScreenScaffold(title = "Add verse", onBack = onDone) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference") },
                placeholder = { Text("John 3:16") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Verse text") },
                placeholder = { Text("Type or paste the verse…") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags") },
                placeholder = { Text("love, faith — comma-separated") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = translation,
                onValueChange = { translation = it },
                label = { Text("Translation (optional)") },
                placeholder = { Text("NWT") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            PrimaryButton(
                text = "Save verse",
                onClick = {
                    store.addVerse(
                        reference = reference,
                        text = text,
                        tags = tags.split(','),
                        translation = translation,
                    )
                    onDone()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
        }
    }
}