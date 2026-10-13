package house.glyph.ui

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
import house.glyph.SettingsStore

/**
 * The help and about page: how a round plays, what the marks mean, the
 * display settings (large text, and the OS magnifiers that work
 * everywhere), and what this game promises. The remembered settings
 * flow in from [settings].
 */
@Composable
fun HelpScreen(settings: SettingsStore, onBack: () -> Unit) {
    ScreenScaffold(title = "Help", onBack = onBack, scrollable = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text(
                "Glyph",
                style = MaterialTheme.readingDisplay,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "A hidden five-letter word, six tries, and a keyboard. " +
                    "Every guess comes back marked — the marks are the clues, " +
                    "and the keyboard remembers everything you've learned.",
                style = MaterialTheme.body,
                color = MaterialTheme.inkColor,
            )

            Text("How to play", style = MaterialTheme.title)
            HelpLines(howToPlay)

            Text("The marks", style = MaterialTheme.title)
            HelpLines(marks)

            Text("Display", style = MaterialTheme.title)
            Text(
                "Large text grows every reading — the tiles, the keyboard, " +
                    "and the type around them — for eyes that want bigger " +
                    "letters.",
                style = MaterialTheme.body,
                color = MaterialTheme.inkColor,
            )
            ChoiceRow(
                label = "Large text",
                selected = settings.largeText,
                onClick = { settings.setLargeText(!settings.largeText) },
            )
            Text(
                "Your device's own magnifier works here too: triple-tap the " +
                    "screen on Android, or press Windows and Plus together on a " +
                    "desktop. Android's system font size setting also grows " +
                    "this game.",
                style = MaterialTheme.label,
                color = MaterialTheme.hintColor,
            )

            Text("About", style = MaterialTheme.title)
            HelpLines(about)
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

private val howToPlay = listOf(
    "• Type a five-letter word and press Enter. Every word in the dictionary is fair — real words only.",
    "• You have six tries. Find the word before they run out; the timer is patience, not clock.",
    "• A fresh word waits behind “New word” whenever you want one — the streak only counts finished rounds.",
    "• On a desktop you can type on a real keyboard; Backspace deletes and Enter guesses.",
)

private val marks = listOf(
    "• Filled green tile — right letter, right place.",
    "• Ember-outlined tile — right letter, wrong place.",
    "• Quiet grey tile — not in the word.",
    "• Duplicate letters score once each, the same as the classic game.",
)

private val about = listOf(
    "• No accounts, no ads, no tracking.",
    "• Works fully offline — the whole dictionary is built in, nothing ever leaves your device.",
    "• Free, today and always.",
    "• Words from the public-domain english-words and google-10000 lists.",
)