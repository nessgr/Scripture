package house.glyph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import house.design.Spacing
import house.design.emberColor
import house.design.hintColor
import house.design.inkColor
import house.design.reference
import house.glyph.domain.LetterMark

/** Conventional corners for tappable affordances — the arch is for cards, not keys. */
private val keyShape = RoundedCornerShape(Spacing.sm)

private val keyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

/**
 * The on-screen keyboard, built by hand because the house kit has no
 * text-input component (and a word game wants keys, not a cursor).
 * Each key wears its letter's best mark so far — the keyboard is the
 * player's memory of what they've learned.
 */
@Composable
internal fun LetterKeyboard(
    marks: Map<Char, LetterMark>,
    enabled: Boolean,
    onKey: (Char) -> Unit,
    onSubmit: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        keyRows.take(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
                row.forEach { letter ->
                    KeyTile(letter, marks[letter], enabled, onKey, Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
            ActionKey("Enter", enabled, onSubmit, Modifier.weight(1.5f))
            keyRows[2].forEach { letter ->
                KeyTile(letter, marks[letter], enabled, onKey, Modifier.weight(1f))
            }
            ActionKey("Back", enabled, onBackspace, Modifier.weight(1.5f))
        }
    }
}

@Composable
private fun KeyTile(
    letter: Char,
    mark: LetterMark?,
    enabled: Boolean,
    onKey: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyBox(
        label = letter.uppercaseChar().toString(),
        background = when (mark) {
            LetterMark.HIT -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        content = when (mark) {
            LetterMark.HIT -> MaterialTheme.colorScheme.onPrimary
            LetterMark.CLOSE -> MaterialTheme.emberColor
            LetterMark.MISS -> MaterialTheme.hintColor
            null -> MaterialTheme.inkColor
        },
        enabled = enabled,
        onClick = { onKey(letter) },
        modifier = modifier,
    )
}

@Composable
private fun ActionKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyBox(
        label = label,
        background = MaterialTheme.colorScheme.surfaceVariant,
        content = MaterialTheme.inkColor,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun KeyBox(
    label: String,
    background: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clip(keyShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(label, style = MaterialTheme.reference, color = content)
    }
}