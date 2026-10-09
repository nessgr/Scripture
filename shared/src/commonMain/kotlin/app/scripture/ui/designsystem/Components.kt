package app.scripture.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import app.scripture.ThemeChoice
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

/** Drill choice label, e.g. ReviewMode.FIRST_LETTERS → "First letters". */
val drillLabel = mapOf(
    app.scripture.domain.ReviewMode.FLASHCARD to "Flashcard",
    app.scripture.domain.ReviewMode.FILL_IN_BLANK to "Fill in the blank",
    app.scripture.domain.ReviewMode.FIRST_LETTERS to "First letters",
    app.scripture.domain.ReviewMode.REVERSE to "Reverse — name the reference",
    app.scripture.domain.ReviewMode.TYPE_WHOLE to "Type the whole verse",
)

/**
 * Standard screen frame: title, optional back, airy padding, and content
 * that never stretches too wide on desktop (max ~560dp reading column).
 * Pass [scrollable] for tall screens whose content must not be clipped —
 * leave it false when the content holds a LazyColumn, which needs bounded
 * height to scroll itself. [trailing] lands at the end of the title row —
 * where the library's theme switch lives.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    scrollable: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        ForeEdge()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                // Android draws edge-to-edge (forced at targetSdk 37); the
                // background keeps painting behind the bars while this keeps
                // content out of them. Desktop has no insets — a no-op there.
                .safeDrawingPadding()
                .then(
                    if (scrollable) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
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
                Text(title, style = MaterialTheme.title, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            content()
        }
    }
}

/** Fore-edge geometry: inset from the window's edge, and the line weight. */
private val ForeEdgeInset = 3.dp
private val ForeEdgeStroke = 2.dp

/**
 * The app's fore-edge: a thin solid rule around the inside of the window,
 * like the ruled border of an old title page — the signature that marks every
 * screen as one of ours. Drawn in code (no image assets) from theme colors,
 * so it follows light and dark. Inset by the system bars — not the keyboard's
 * insets — so edge-to-edge Android doesn't bury it behind the status bar, and
 * typing doesn't make the rule jump.
 */
@Composable
private fun ForeEdge() {
    val ink = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .drawBehind { drawForeEdge(ink, ForeEdgeInset, ForeEdgeStroke) },
    )
}

/** The frame drawn as one stroke — all four edges share its corners. */
private fun DrawScope.drawForeEdge(
    ink: Color,
    inset: Dp,
    stroke: Dp,
) {
    val i = inset.toPx()
    drawRoundRect(
        color = ink,
        topLeft = Offset(i, i),
        size = Size(size.width - 2 * i, size.height - 2 * i),
        cornerRadius = CornerRadius(i),
        style = Stroke(width = stroke.toPx()),
    )
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

/** Toggle labels, e.g. ThemeChoice.SYSTEM → "Auto". */
val themeToggleLabel = mapOf(
    ThemeChoice.SYSTEM to "Auto",
    ThemeChoice.LIGHT to "Light",
    ThemeChoice.DARK to "Dark",
)

/**
 * The three-way light switch, as a compact segmented control: a pill track
 * in surfaceVariant, the chosen segment filled with accent. Text, not
 * sun/moon glyphs — glyphs render differently on every platform, and the
 * app's voice is type.
 */
@Composable
fun ThemeToggle(
    choice: ThemeChoice,
    onChoice: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.xs),
    ) {
        ThemeChoice.entries.forEach { option ->
            val selected = option == choice
            Text(
                text = themeToggleLabel.getValue(option),
                style = MaterialTheme.label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onChoice(option) }
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }
    }
}

/**
 * Verse text with an illuminated drop cap: the first letter oversized in
 * Lora and accent-colored, the rest in the scripture voice. Surrogate
 * pairs are kept whole so an emoji first character can't be split.
 */
@Composable
fun DropCapText(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isEmpty()) return
    val capSpan = MaterialTheme.dropCap
        .toSpanStyle()
        .copy(color = MaterialTheme.colorScheme.primary)
    val capEnd = if (text.length >= 2 && text[0].isHighSurrogate()) 2 else 1
    Text(
        text = buildAnnotatedString {
            withStyle(capSpan) { append(text.substring(0, capEnd)) }
            append(text.substring(capEnd))
        },
        style = MaterialTheme.scriptureBody,
        color = MaterialTheme.scriptureColor,
        modifier = modifier,
    )
}

/**
 * Small pill for a verse tag. Doubles as the library's tag-filter control:
 * pass [onClick] to make it tappable, [selected] for the filled "filtering by
 * this tag" look. Hand-styled to keep the pill shape — Material3's FilterChip
 * would clash with the rest of the design.
 */
@Composable
fun TagChip(
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    Text(
        text = label,
        style = MaterialTheme.label,
        color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary,
        modifier = (
            if (selected) {
                Modifier
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary)
            } else {
                Modifier
                    .clip(shape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            }
            ).let { base ->
                if (onClick != null) base.clickable(onClick = onClick) else base
            }
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}

/**
 * One row of a single-choice group: a radio button and its label, the whole
 * row tappable. Selection state stays with the caller — rows are pure.
 */
@Composable
fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.body, modifier = Modifier.padding(start = Spacing.xs))
    }
}

/**
 * A verse in the library list: reference, serif preview, tags. Give it an
 * [onClick] to make the whole card open the verse detail screen — the
 * clickable Card overload is the same one ClickableCard uses, so ripple
 * and semantics stay consistent across the app.
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
                color = MaterialTheme.hintColor,
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
        shape = archShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            content = content,
            modifier = Modifier.padding(Spacing.xl),
        )
    }
}