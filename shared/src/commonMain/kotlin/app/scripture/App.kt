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
import app.scripture.ui.screens.LibraryScreen
import app.scripture.ui.screens.ReviewScreen

/**
 * Navigation: three screens, hand-rolled. A sealed class means the compiler
 * proves the `when` covers every screen — no navigation library needed yet.
 */
sealed class Screen {
    data object Library : Screen()
    data object AddVerse : Screen()
    data object Review : Screen()
}

/** Root composable shared by both entry points (Android + desktop). */
@Composable
fun App() {
    val store = remember { ScriptureStore(VerseRepository(defaultDataStorage())) }
    var screen by remember { mutableStateOf<Screen>(Screen.Library) }

    LaunchedEffect(store) { store.load() }

    ScriptureTheme {
        when (screen) {
            Screen.Library -> LibraryScreen(
                store = store,
                onAddVerse = { screen = Screen.AddVerse },
                onStartReview = {
                    store.startReview()
                    screen = Screen.Review
                },
            )
            Screen.AddVerse -> AddVerseScreen(
                store = store,
                onDone = { screen = Screen.Library },
            )
            Screen.Review -> ReviewScreen(
                store = store,
                onExit = { screen = Screen.Library },
            )
        }
    }
}