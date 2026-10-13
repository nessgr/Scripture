package app.scripture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.scripture.data.VerseRepository
import app.scripture.data.defaultDataStorage
import app.scripture.domain.ReviewMode
import app.scripture.ui.screens.AboutScreen
import app.scripture.ui.screens.AddVerseScreen
import app.scripture.ui.screens.EditVerseScreen
import app.scripture.ui.screens.ImportScreen
import app.scripture.ui.screens.LibraryScreen
import app.scripture.ui.screens.PracticeScreen
import app.scripture.ui.screens.ReviewScreen
import app.scripture.ui.screens.VerseDetailScreen
import house.design.HouseTheme

/**
 * Navigation: hand-rolled. A sealed class means the compiler proves the
 * `when` covers every screen — no navigation library needed yet. Screens
 * that need a verse carry its id (never the object — the store is the
 * source of truth and a snapshot would go stale).
 */
sealed class Screen {
    data object Library : Screen()
    data object AddVerse : Screen()
    data object Import : Screen()
    data object About : Screen()
    data class VerseDetail(val verseId: String) : Screen()
    data class EditVerse(val verseId: String) : Screen()
    data class Practice(val verseId: String, val mode: ReviewMode) : Screen()
    data object Review : Screen()
}

/** Root composable shared by both entry points (Android + desktop). */
@Composable
fun App() {
    val storage = remember { defaultDataStorage() }
    val store = remember { ScriptureStore(VerseRepository(storage)) }
    val themeStore = remember { ThemeStore(storage) }
    var screen by remember { mutableStateOf<Screen>(Screen.Library) }

    val themeChoice by themeStore.choice.collectAsState()

    LaunchedEffect(store) { store.load() }

    HouseTheme(choice = themeChoice) {
        // The app's one motion signature: a short crossfade. Alpha-only —
        // cheap on every platform, calm by design. AnimatedContent hands
        // the current target to the lambda as a plain val, so the
        // is-branches below can smart-cast the verseId out of it.
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.Library -> LibraryScreen(
                    store = store,
                    themeChoice = themeChoice,
                    onThemeChoice = { themeStore.set(it) },
                    onAddVerse = { screen = Screen.AddVerse },
                    onStartReview = {
                        store.startReview()
                        screen = Screen.Review
                    },
                    onOpenVerse = { verseId -> screen = Screen.VerseDetail(verseId) },
                    onImport = { screen = Screen.Import },
                    onAbout = { screen = Screen.About },
                )
                Screen.About -> AboutScreen(onBack = { screen = Screen.Library })
                Screen.AddVerse -> AddVerseScreen(
                    store = store,
                    onDone = { screen = Screen.Library },
                )
                Screen.Import -> ImportScreen(
                    store = store,
                    onDone = { screen = Screen.Library },
                )
                is Screen.VerseDetail -> VerseDetailScreen(
                    store = store,
                    verseId = current.verseId,
                    onBack = { screen = Screen.Library },
                    onEdit = { verseId -> screen = Screen.EditVerse(verseId) },
                    onPractice = { verseId, mode -> screen = Screen.Practice(verseId, mode) },
                )
                is Screen.Practice -> {
                    val verseId = current.verseId
                    PracticeScreen(
                        store = store,
                        verseId = verseId,
                        mode = current.mode,
                        // Practice exits back to the verse it came from
                        onExit = { screen = Screen.VerseDetail(verseId) },
                    )
                }
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
}