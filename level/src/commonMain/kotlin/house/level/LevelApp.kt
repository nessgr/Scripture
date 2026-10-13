package house.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import house.design.ClickableCard
import house.design.ChoiceRow
import house.design.DropCapText
import house.design.EmptyState
import house.design.HouseTheme
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing
import house.design.TagChip
import house.design.ThemeChoice
import house.design.ThemeToggle
import house.design.body
import house.design.hintColor
import house.design.title

/**
 * The Level: a one-screen showcase of the house design kit. The theme
 * toggle is real (though not persisted yet — no store in the shell), and
 * every visible element comes from the kit. This screen is the proving
 * ground for the shared style, and the seed the real app will grow from.
 */
@Composable
fun LevelApp() {
    var choice by remember { mutableStateOf(ThemeChoice.SYSTEM) }
    HouseTheme(choice = choice) {
        ScreenScaffold(
            title = "Level",
            scrollable = true,
            trailing = { ThemeToggle(choice = choice, onChoice = { choice = it }) },
        ) {
            Text("House style, worn by a second app.", style = MaterialTheme.title)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Everything below comes from :designkit — the Scripture app wears the identical kit.",
                style = MaterialTheme.body,
                color = MaterialTheme.hintColor,
            )
            Spacer(Modifier.height(Spacing.lg))

            DropCapText("The quick brown fox jumps over the lazy dog.")
            Spacer(Modifier.height(Spacing.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TagChip("Tag")
                TagChip("Selected", selected = true)
                TagChip("Tappable", onClick = {})
            }
            Spacer(Modifier.height(Spacing.lg))

            PrimaryButton(text = "Primary action", onClick = {})
            Spacer(Modifier.height(Spacing.lg))

            ClickableCard(onClick = {}) {
                Text("Arch card", style = MaterialTheme.title)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "A card in the house's signature silhouette — click anywhere in it.",
                    style = MaterialTheme.body,
                )
            }
            Spacer(Modifier.height(Spacing.lg))

            var radio by remember { mutableStateOf("a") }
            ChoiceRow(label = "One option", selected = radio == "a", onClick = { radio = "a" })
            ChoiceRow(label = "Another option", selected = radio == "b", onClick = { radio = "b" })
            Spacer(Modifier.height(Spacing.lg))

            EmptyState(
                message = "Nothing here yet",
                hint = "This screen will grow into the real app.",
            )
        }
    }
}