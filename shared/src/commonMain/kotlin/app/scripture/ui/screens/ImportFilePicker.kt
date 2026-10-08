package app.scripture.ui.screens

import androidx.compose.runtime.Composable

/** What picking a file produced. */
sealed interface PickOutcome {
    /** The file's contents as text — landed in the paste field for review. */
    data class Read(val text: String) : PickOutcome
    data object Cancelled : PickOutcome
    /** Chosen but couldn't be read (missing, unreadable, or too large). */
    data object Unreadable : PickOutcome
}

/**
 * Opens the platform's choose-a-file dialog and reads the file as text.
 * The result always funnels into the same paste field, so there is exactly
 * one path from raw text to imported verses.
 */
interface ImportFilePicker {
    suspend fun pick(): PickOutcome
}

/** The platform's file picker: SAF document picker on Android, Swing dialog on desktop. */
@Composable
expect fun rememberImportFilePicker(): ImportFilePicker