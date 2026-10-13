package house.level

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import house.design.HouseTheme
import house.design.ThemeChoice
import house.level.ui.LevelScreen

/**
 * The Level app: the phone as a spirit level — two vials at right angles,
 * a bullseye in the middle, zero calibration, degrees read out.
 */
@Composable
fun LevelApp() {
    var choice by remember { mutableStateOf(ThemeChoice.SYSTEM) }
    HouseTheme(choice = choice) {
        LevelScreen(choice = choice, onChoice = { choice = it })
    }
}