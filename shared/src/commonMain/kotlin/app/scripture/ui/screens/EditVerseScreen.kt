package app.scripture.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.scripture.ScriptureStore
import house.design.EmptyState
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing

/**
 * Edit form for one verse. Only content changes — id, creation date, and
 * review progress are preserved by the store's updateVerse.
 */
@Composable
fun EditVerseScreen(
    store: ScriptureStore,
    verseId: String,
    onDone: () -> Unit,
) {
    val verses by store.verses.collectAsState()
    val verse = verses.firstOrNull { it.id == verseId }

    if (verse == null) {
        ScreenScaffold(title = "Edit verse", onBack = onDone) {
            EmptyState("Verse not found", "It may have been deleted.")
        }
        return
    }

    // Keyed by id so the form survives store updates while editing; a fresh
    // verse object with the same id keeps what the user has typed.
    key(verse.id) {
        val form = remember {
            VerseFormState(
                initialReference = verse.reference,
                initialText = verse.text,
                initialTags = verse.tags.joinToString(", "),
                initialTranslation = verse.translation.orEmpty(),
            )
        }

        ScreenScaffold(title = "Edit verse", onBack = onDone) {
            VerseForm(form)

            PrimaryButton(
                text = "Save changes",
                onClick = {
                    store.updateVerse(
                        verseId = verse.id,
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
}