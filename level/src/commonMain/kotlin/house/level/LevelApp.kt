package house.level

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import house.design.HouseTheme
import house.level.data.defaultDataStorage
import house.level.ui.HelpScreen
import house.level.ui.LevelScreen

/** Where the user is: the instrument, or the help page. */
private enum class LevelPage { Instrument, Help }

/**
 * The Level app: the phone as a spirit level — two vials at right angles,
 * a bullseye in the middle, zero calibration, and a rise calculator that
 * turns tilt into how much to lift. One DataStorage feeds the settings
 * store (theme, large text, run, units) and the level store's persisted
 * zero; both are plain files per platform, remembered across launches.
 */
@Composable
fun LevelApp() {
    val storage = remember { defaultDataStorage() }
    val settings = remember { SettingsStore(storage) }
    val current by settings.settings.collectAsState()
    var page by remember { mutableStateOf(LevelPage.Instrument) }
    HouseTheme(choice = settings.choice, largeText = current.largeText) {
        when (page) {
            LevelPage.Instrument -> LevelScreen(
                settings = settings,
                storage = storage,
                onHelp = { page = LevelPage.Help },
            )
            LevelPage.Help -> HelpScreen(
                settings = settings,
                onBack = { page = LevelPage.Instrument },
            )
        }
    }
}