package house.level.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import house.design.ChoiceRow
import house.design.ScreenScaffold
import house.design.Spacing
import house.design.body
import house.design.hintColor
import house.design.inkColor
import house.design.label
import house.design.readingDisplay
import house.design.title

/**
 * The help and about page: how to read the instrument, how the rise
 * calculator turns tilt into lift, the display settings (large text, and
 * the OS magnifiers that work everywhere), and what this app promises.
 */
@Composable
fun HelpScreen(onBack: () -> Unit, largeText: Boolean, onLargeText: (Boolean) -> Unit) {
    ScreenScaffold(title = "Help", onBack = onBack, scrollable = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text(
                "Level",
                style = MaterialTheme.readingDisplay,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Your phone as a spirit level — two vials at right angles, a " +
                    "bullseye in the middle, and a rise calculator for the " +
                    "question a level alone can't answer: how much to lift.",
                style = MaterialTheme.body,
                color = MaterialTheme.inkColor,
            )

            Text("Reading the instrument", style = MaterialTheme.title)
            HelpLines(readingHelp)

            Text("The rise calculator", style = MaterialTheme.title)
            HelpLines(riseHelp)

            Text("Calibration", style = MaterialTheme.title)
            HelpLines(calibrationHelp)

            Text("Display", style = MaterialTheme.title)
            Text(
                "Large text grows every reading — the buttons, the vials, and " +
                    "a big degrees-and-rise bar under the instrument — for eyes " +
                    "that want bigger numbers.",
                style = MaterialTheme.body,
                color = MaterialTheme.inkColor,
            )
            ChoiceRow(
                label = "Large text",
                selected = largeText,
                onClick = { onLargeText(!largeText) },
            )
            Text(
                "Your device's own magnifier works here too: triple-tap the " +
                    "screen on Android, or press Windows and Plus together on a " +
                    "desktop. Android's system font size setting also grows " +
                    "this app.",
                style = MaterialTheme.label,
                color = MaterialTheme.hintColor,
            )

            Text("About", style = MaterialTheme.title)
            HelpLines(promises)
        }
    }
}

/** A plain bulleted list — the house voice for instructions and promises. */
@Composable
private fun HelpLines(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        lines.forEach { line ->
            Text(line, style = MaterialTheme.body, color = MaterialTheme.inkColor)
        }
    }
}

private val readingHelp = listOf(
    "• Lay the phone flat on the surface you're leveling — a camper floor, a counter, a shelf.",
    "• The vial across the top reads side-to-side; the one down the side reads front-to-back. The bubble drifts toward the high side.",
    "• You're level when the bubble nests between its marks and the bullseye's dot rests inside the center ring. Readings within half a degree turn ember to say so.",
)

private val riseHelp = listOf(
    "• The run is the distance between the two points that carry the surface — tires seven feet apart are a run of 7.",
    "• Each vial engraves its angle in degrees; once a run is set, the line beneath is the rise — how far to lift the low side: run × tan(angle).",
    "• Feet report in inches, meters in centimeters — the working numbers under a camper.",
)

private val calibrationHelp = listOf(
    "• A surface you trust as flat can become ground truth: press Zero here while resting on it, and every reading — degrees and rise — is measured from it.",
    "• Clear zero returns to gravity as the only truth.",
)

private val promises = listOf(
    "• No accounts, no ads, no tracking.",
    "• Works fully offline — nothing ever leaves your phone.",
    "• Free, today and always.",
)