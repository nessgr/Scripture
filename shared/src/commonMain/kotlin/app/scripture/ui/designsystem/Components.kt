package app.scripture.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.scripture.domain.Verse

/*
 * The reusable components every screen is built from. All colors, fonts,
 * and spacing come from Theme.kt / Tokens.kt — nothing is hardcoded here.
 */

/** Grade button label, e.g. Grade.GOOD → "Good". */
val gradeLabel = mapOf(
    app.scripture.domain.Grade.AGAIN to "Again",
    app.scripture.domain.Grade.HARD to "Hard",
    app.scripture.domain.Grade.GOOD to "Good",
    app.scripture.domain.Grade.EASY to "Easy",
)

/**
 * Standard screen frame: title, optional back, airy padding, and content
 * that never stretches too wide on desktop (max ~560dp reading column).
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(Spacing.xl),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
            ) {
                if (onBack != null) {
                    OutlinedButton(
                        onClick = onBack,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = Spacing.sm, vertical = Spacing.xs,
                        ),
                    ) { Text("←") }
                    Spacer(Modifier.width(Spacing.md))
                }
                Text(title, style = MaterialTheme.title)
            }
            content()
        }
    }
}

/** Accent-filled action button. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(text, style = MaterialTheme.body)
    }
}

/** Small outlined pill for a verse tag. */
@Composable
fun TagChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.label,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}

/** A verse in the library list: reference, serif preview, tags. */
@Composable
fun VerseCard(verse: Verse, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
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
                style = MaterialTheme.scriptureBody,
                color = MaterialTheme.scriptureColor,
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
}

/**
 * The four SRS grade buttons with a preview of the interval each choice
 * produces ("Good · 6d"), so the user understands what they're choosing.
 */
@Composable
fun GradeBar(
    preview: (app.scripture.domain.Grade) -> String,
    onGrade: (app.scripture.domain.Grade) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        app.scripture.domain.Grade.entries.forEach { grade ->
            OutlinedButton(
                onClick = { onGrade(grade) },
                shape = MaterialTheme.shapes.medium,
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

/** Calm message when there is nothing to show. */
@Composable
fun EmptyState(message: String, hint: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xxl),
    ) {
        Text(message, style = MaterialTheme.title, textAlign = TextAlign.Center)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.body,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Click-anywhere wrapper used by the flashcard reveal. */
@Composable
fun ClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            content = content,
            modifier = Modifier.padding(Spacing.xl),
        )
    }
}