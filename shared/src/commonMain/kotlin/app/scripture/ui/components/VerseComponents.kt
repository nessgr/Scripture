package app.scripture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.scripture.domain.Grade
import app.scripture.domain.ReviewMode
import app.scripture.domain.Verse
import house.design.Spacing
import house.design.TagChip
import house.design.archShape
import house.design.body
import house.design.inkColor
import house.design.label
import house.design.readingBody
import house.design.reference

/*
 * Scripture-specific components — everything here is built from the house
 * kit (house.design) and knows this app's domain. The kit stays app-agnostic.
 */

/** Grade button label, e.g. Grade.GOOD → "Good". */
val gradeLabel = mapOf(
    Grade.AGAIN to "Again",
    Grade.HARD to "Hard",
    Grade.GOOD to "Good",
    Grade.EASY to "Easy",
)

/** Drill choice label, e.g. ReviewMode.FIRST_LETTERS → "First letters". */
val drillLabel = mapOf(
    ReviewMode.FLASHCARD to "Flashcard",
    ReviewMode.FILL_IN_BLANK to "Fill in the blank",
    ReviewMode.FIRST_LETTERS to "First letters",
    ReviewMode.REVERSE to "Reverse — name the reference",
    ReviewMode.TYPE_WHOLE to "Type the whole verse",
)

/**
 * A verse in the library list: reference, reading-voice preview, tags. Give
 * it an [onClick] to make the whole card open the verse detail screen — the
 * clickable Card overload is the same one the kit's ClickableCard uses, so
 * ripple and semantics stay consistent across the app.
 */
@Composable
fun VerseCard(
    verse: Verse,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(Spacing.lg),
        ) {
            Text(
                verse.reference.uppercase(),
                style = MaterialTheme.reference,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                verse.text,
                style = MaterialTheme.readingBody,
                color = MaterialTheme.inkColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (verse.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    verse.tags.take(4).forEach { TagChip(it) }
                }
            }
        }
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = archShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier.fillMaxWidth(),
            content = content,
        )
    } else {
        Card(
            shape = archShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier.fillMaxWidth(),
            content = content,
        )
    }
}

/**
 * The four SRS grade buttons with a preview of the interval each choice
 * produces ("Good · 6d"), so the user understands what they're choosing.
 * [suggested] is the grade the drill's measured accuracy points to — it
 * gets the accent border, and stays a one-tap confirmation, not an
 * autopilot: honest self-assessment can always overrule the machine.
 */
@Composable
fun GradeBar(
    preview: (Grade) -> String,
    onGrade: (Grade) -> Unit,
    suggested: Grade? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Grade.entries.forEach { grade ->
            OutlinedButton(
                onClick = { onGrade(grade) },
                shape = MaterialTheme.shapes.medium,
                border = if (grade == suggested) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null // the default outlined border
                },
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(gradeLabel.getValue(grade), style = MaterialTheme.body)
                    Text(
                        preview(grade),
                        style = MaterialTheme.label,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}