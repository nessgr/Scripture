package house.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Named tokens — the vocabulary screens are allowed to use.
 * `MaterialTheme.readingBody` reads nicer than remembering that our reading
 * 18sp lives in `typography.bodyLarge`. The getters are @Composable because
 * they read the theme, like MaterialTheme.typography itself does.
 */

/** Spacing scale. Airy layout: default to `xl` for screens, `lg` for cards. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

/** Reading voice, 22sp — the big display style for long-form openings. */
val MaterialTheme.readingDisplay: TextStyle
    @Composable get() = typography.displayMedium

/** Reading voice, 18sp — regular long-form reading text. */
val MaterialTheme.readingBody: TextStyle
    @Composable get() = typography.bodyLarge

/** UI voice, 13sp letterspaced label, e.g. "JOHN 3:16". */
val MaterialTheme.reference: TextStyle
    @Composable get() = typography.labelLarge

/** UI voice, 17sp — screen titles. */
val MaterialTheme.title: TextStyle
    @Composable get() = typography.titleLarge

/** UI voice, 15sp — ordinary UI body text. */
val MaterialTheme.body: TextStyle
    @Composable get() = typography.bodyMedium

/** UI voice, 12sp — small labels, badges, hints. */
val MaterialTheme.label: TextStyle
    @Composable get() = typography.labelMedium

/** Reading voice, 42sp — the drop cap (see DropCapText). */
val MaterialTheme.dropCap: TextStyle
    @Composable get() = typography.headlineMedium

/** Deep "printed ink" color for long-form reading text (see Theme.kt). */
val MaterialTheme.inkColor: Color
    @Composable get() = LocalExtendedColors.current.reading

/** Quiet-but-readable color for hints and metadata (see Theme.kt). */
val MaterialTheme.hintColor: Color
    @Composable get() = LocalExtendedColors.current.hint

/** Warm ochre emphasis — right word, rough edges; "so close". */
val MaterialTheme.emberColor: Color
    @Composable get() = LocalExtendedColors.current.ember

/**
 * The house's signature silhouette: an arch — tall radius overhead, quiet
 * footing below, like a window in an illuminated manuscript. Cards wear
 * it; buttons don't, so tappable affordances stay conventional.
 */
val archShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)