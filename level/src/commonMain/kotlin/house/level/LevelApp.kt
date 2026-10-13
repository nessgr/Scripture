package house.level

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import house.design.HouseTheme
import house.design.ThemeChoice
import house.level.ui.HelpScreen
import house.level.ui.LevelScreen

/** Where the user is: the instrument, or the help page. */
private enum class LevelPage { Instrument, Help }

/**
 * The Level app: the phone as a spirit level — two vials at right angles,
 * a bullseye in the middle, zero calibration, and a rise calculator that
 * turns tilt into how much to lift. Large text (the house accessibility
 * mode) grows every reading; the setting lives on the help page.
 */
@Composable
fun LevelApp() {
    var choice by remember { mutableStateOf(ThemeChoice.SYSTEM) }
    var largeText by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(LevelPage.Instrument) }
    HouseTheme(choice = choice, largeText = largeText) {
        when (page) {
            LevelPage.Instrument -> LevelScreen(
                choice = choice,
                onChoice = { choice = it },
                onHelp = { page = LevelPage.Help },
                largeText = largeText,
            )
            LevelPage.Help -> HelpScreen(
                onBack = { page = LevelPage.Instrument },
                largeText = largeText,
                onLargeText = { largeText = it },
            )
        }
    }
}