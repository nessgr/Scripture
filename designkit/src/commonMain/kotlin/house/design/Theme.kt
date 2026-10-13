package house.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import house.designkit.generated.resources.Res
import house.designkit.generated.resources.inter_italic
import house.designkit.generated.resources.inter_regular
import house.designkit.generated.resources.inter_semibold
import house.designkit.generated.resources.space_grotesk_medium
import house.designkit.generated.resources.space_grotesk_regular
import house.designkit.generated.resources.space_grotesk_semibold

/*
 * HOUSE DESIGN SYSTEM — the one place the shared look lives.
 *
 * To restyle every house app: change colors here, swap font files in
 * composeResources/font, or adjust Spacing/Type in Tokens.kt. Screens must
 * never hardcode a color, font, or padding value — they only read tokens.
 */

/** One palette definition, from which both Material schemes are built. */
data class Palette(
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    /** Deep "printed ink" color for long-form reading text — extends Material's roles. */
    val reading: Color,
    /** Readable ochre for warm emphasis — right answer, rough edges; "so close". */
    val ember: Color,
)

private val LightPalette = Palette(
    accent = Color(0xFF4A6B5A),      // muted sage
    onAccent = Color(0xFFFFFFFF),
    background = Color(0xFFFAF9F6),   // warm near-white
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFEFEEE8),
    outline = Color(0xFFD8D6CE),
    reading = Color(0xFF23221D),
    ember = Color(0xFF946B2D),
)

private val DarkPalette = Palette(
    accent = Color(0xFFA3C2B0),       // light sage, readable on dark
    onAccent = Color(0xFF16211B),
    background = Color(0xFF131412),
    onBackground = Color(0xFFE8E6E0),
    surface = Color(0xFF1B1D1A),
    onSurface = Color(0xFFE8E6E0),
    surfaceVariant = Color(0xFF242622),
    outline = Color(0xFF4A4D47),      // borders only — still quiet, but OutlinedButtons read
    reading = Color(0xFFDCDAD2),
    ember = Color(0xFFD9B36A),
)

private fun scheme(p: Palette): ColorScheme = lightColorScheme(
    primary = p.accent,
    onPrimary = p.onAccent,
    background = p.background,
    onBackground = p.onBackground,
    surface = p.surface,
    onSurface = p.onSurface,
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurface,
    outline = p.outline,
)

/**
 * The Material roles Material3 does not have: a text color dedicated to
 * long-form reading, and a readable "quiet" color for hints and metadata.
 * Provided via CompositionLocal — the documented M3 pattern for extending
 * the theme.
 *
 * [hint] is deliberately NOT `outline`: outline is a border color, far too
 * low-contrast for text (unreadably so on the dark palette). Hints dim by
 * alpha over the readable ink instead.
 */
data class ExtendedColors(
    val reading: Color,
    val hint: Color,
    val ember: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        reading = LightPalette.reading,
        hint = LightPalette.onBackground.copy(alpha = 0.62f),
        ember = LightPalette.ember,
    )
}

/**
 * Bundled fonts (composeResources/font) render identically on Android and
 * desktop — no platform-font roulette. Inter speaks for long-form reading
 * (clean, modern, screen-perfect); Space Grotesk for UI (a quirky grotesque
 * that gives the chrome its own voice). Loading a resource font is a
 * @Composable call, so the families are built here rather than as
 * top-level vals.
 */
@Composable
private fun readingTypography(): Typography {
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val spaceGrotesk = FontFamily(
        Font(Res.font.space_grotesk_regular, FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
    )
    return buildTypography(inter, spaceGrotesk)
}

/** Material has no reading slot, so map the M3 type scale onto our two
 *  voices: reading and UI. Named aliases live in Tokens.kt. */
private fun buildTypography(reading: FontFamily, ui: FontFamily): Typography = Typography(
    displayMedium = TextStyle(
        fontFamily = reading, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        // The drop cap: two body lines tall, so the first letter of a text
        // opens like an illuminated manuscript.
        fontFamily = reading, fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp, lineHeight = 42.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = reading, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 30.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 1.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
)

/** Root of every screen: wrap your composable in this and get the full look.
 *  [choice] decides which palette is used — SYSTEM follows the platform. */
@Composable
fun HouseTheme(
    choice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val palette = if (darkTheme) DarkPalette else LightPalette
    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkColorScheme(
                primary = palette.accent,
                onPrimary = palette.onAccent,
                background = palette.background,
                onBackground = palette.onBackground,
                surface = palette.surface,
                onSurface = palette.onSurface,
                surfaceVariant = palette.surfaceVariant,
                onSurfaceVariant = palette.onSurface,
                outline = palette.outline,
            )
        } else {
            scheme(palette)
        },
        typography = readingTypography(),
    ) {
        CompositionLocalProvider(
            LocalExtendedColors provides ExtendedColors(
                reading = palette.reading,
                hint = palette.onBackground.copy(alpha = 0.62f),
                ember = palette.ember,
            ),
            // Text with no explicit color falls back to LocalContentColor, whose
            // Material3 default is BLACK — unreadable on the dark palette. A root
            // Surface would provide this; our scaffold paints its own background
            // instead, so the theme must set it. This is what keeps uncolored
            // Text (titles, labels, empty states) readable in dark mode.
            LocalContentColor provides palette.onBackground,
        ) {
            content()
        }
    }
}