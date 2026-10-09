package app.scripture.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Named tokens — the vocabulary screens are allowed to use.
 * `MaterialTheme.scriptureBody` reads nicer than remembering that our serif
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

/** Serif, 22sp — the big "memorize this" verse display. */
val MaterialTheme.scriptureDisplay: TextStyle
    @Composable get() = typography.displayMedium

/** Serif, 18sp — regular scripture reading text. */
val MaterialTheme.scriptureBody: TextStyle
    @Composable get() = typography.bodyLarge

/** Sans, 13sp letterspaced reference label, e.g. "JOHN 3:16". */
val MaterialTheme.reference: TextStyle
    @Composable get() = typography.labelLarge

/** Sans, 17sp — screen titles. */
val MaterialTheme.title: TextStyle
    @Composable get() = typography.titleLarge

/** Sans, 15sp — ordinary UI body text. */
val MaterialTheme.body: TextStyle
    @Composable get() = typography.bodyMedium

/** Sans, 12sp — small labels, badges, hints. */
val MaterialTheme.label: TextStyle
    @Composable get() = typography.labelMedium

/** Serif, 42sp — the drop cap (see DropCapText). */
val MaterialTheme.dropCap: TextStyle
    @Composable get() = typography.headlineMedium

/** The custom scripture text color (see Theme.kt). */
val MaterialTheme.scriptureColor: Color
    @Composable get() = LocalExtendedColors.current.scripture

/** Quiet-but-readable color for hints and metadata (see Theme.kt). */
val MaterialTheme.hintColor: Color
    @Composable get() = LocalExtendedColors.current.hint

/**
 * The app's signature silhouette: an arch — tall radius overhead, quiet
 * footing below, like a window in an illuminated manuscript. Cards wear
 * it; buttons don't, so tappable affordances stay conventional.
 */
val archShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)