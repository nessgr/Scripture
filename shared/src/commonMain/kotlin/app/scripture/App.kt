package app.scripture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.scripture.data.VerseRepository
import app.scripture.data.defaultDataStorage
import app.scripture.ui.designsystem.ScriptureTheme
import app.scripture.ui.screens.AddVerseScreen
import app.scripture.ui.screens.EditVerseScreen
import app.scripture.ui.screens.LibraryScreen
import app.scripture.ui.screens.ReviewScreen
import app.scripture.ui.screens.VerseDetailScreen

/**
 * Navigation: hand-rolled. A sealed class means the compiler proves the
 * `when` covers every screen — no navigation library needed yet. Screens
 * that need a verse carry its id (never the object — the store is the
 * source of truth and a snapshot would go stale).
 */
sealed class Screen {
    data object Library : Screen()
    data object AddVerse : Screen()
    data class VerseDetail(val verseId: String) : Screen()
    data class EditVerse(val verseId: String) : Screen()
    data object Review : Screen()
}

/** Root composable shared by both entry points (Android + desktop). */
@Composable
fun App() {
    val store = remember { ScriptureStore(VerseRepository(defaultDataStorage())) }
    var screen by remember { mutableStateOf<Screen>(Screen.Library) }

    LaunchedEffect(store) { store.load() }

    ScriptureTheme {
        // Captured into a plain val: a delegated `by remember` var can't be
        // smart-cast, and the is-branches need verseId.
        when (val current = screen) {
            Screen.Library -> LibraryScreen(
                store = store,
                onAddVerse = { screen = Screen.AddVerse },
                onStartReview = {
                    store.startReview()
                    screen = Screen.Review
                },
                onOpenVerse = { verseId -> screen = Screen.VerseDetail(verseId) },
            )
            Screen.AddVerse -> AddVerseScreen(
                store = store,
                onDone = { screen = Screen.Library },
            )
            is Screen.VerseDetail -> VerseDetailScreen(
                store = store,
                verseId = current.verseId,
                onBack = { screen = Screen.Library },
                onEdit = { verseId -> screen = Screen.EditVerse(verseId) },
            )
            is Screen.EditVerse -> {
                val verseId = current.verseId
                EditVerseScreen(
                    store = store,
                    verseId = verseId,
                    // Saving or backing out returns to the detail screen it came from
                    onDone = { screen = Screen.VerseDetail(verseId) },
                )
            }
            Screen.Review -> ReviewScreen(
                store = store,
                onExit = { screen = Screen.Library },
            )
        }
    }
}