package house.glyph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import house.design.HouseTheme
import house.glyph.data.defaultDataStorage
import house.glyph.ui.GameScreen
import house.glyph.ui.HelpScreen

/**
 * The game's root: own the storage and the stores once, then hand them
 * down. Screens stay stateless in their seams — the theme wraps both
 * pages, so large text and the theme choice follow the player everywhere.
 */
@Composable
fun GlyphApp() {
    val storage = remember { defaultDataStorage() }
    val settings = remember { SettingsStore(storage) }
    val stats = remember { StatsStore(storage) }
    val games = remember { GameStore(storage, stats) }
    val current by settings.settings.collectAsState()
    var page by remember { mutableStateOf(GlyphPage.Game) }
    HouseTheme(choice = settings.choice, largeText = current.largeText) {
        when (page) {
            GlyphPage.Game -> GameScreen(
                settings = settings,
                games = games,
                stats = stats,
                onHelp = { page = GlyphPage.Help },
            )
            GlyphPage.Help -> HelpScreen(settings = settings, onBack = { page = GlyphPage.Game })
        }
    }
}

private enum class GlyphPage { Game, Help }