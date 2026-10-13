package app.scripture.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.scripture.ScriptureStore
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing

/** Form for adding one verse. Everything is optional except reference + text. */
@Composable
fun AddVerseScreen(
    store: ScriptureStore,
    onDone: () -> Unit,
) {
    val form = remember { VerseFormState() }

    ScreenScaffold(title = "Add verse", onBack = onDone) {
        VerseForm(form)

        PrimaryButton(
            text = "Save verse",
            onClick = {
                store.addVerse(
                    reference = form.reference,
                    text = form.text,
                    tags = form.tagList,
                    translation = form.translation,
                )
                onDone()
            },
            enabled = form.canSave,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}