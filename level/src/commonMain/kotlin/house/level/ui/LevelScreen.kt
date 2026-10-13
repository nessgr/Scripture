package house.level.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing
import house.design.ThemeChoice
import house.design.ThemeToggle
import house.design.emberColor
import house.design.hintColor
import house.design.reference
import house.level.LevelStore
import house.level.data.defaultTiltSource
import house.level.domain.formatDegrees
import house.level.domain.isLevel
import house.level.domain.Tilt

/**
 * The instrument screen: the level square, its two degree readouts, and the
 * zero control. Tilt arrives from the store; drawing stays in
 * LevelDrawing.kt, readouts and buttons live here. [choice]/[onChoice] are
 * the theme toggle that lives in the scaffold's title row.
 */
@Composable
fun LevelScreen(choice: ThemeChoice, onChoice: (ThemeChoice) -> Unit) {
    val source = remember { defaultTiltSource() }
    val store = remember { LevelStore(source) }
    val tilt by store.tilt.collectAsState()
    val zero by store.zero.collectAsState()

    ScreenScaffold(
        title = "Level",
        trailing = { ThemeToggle(choice = choice, onChoice = onChoice) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            LevelSquare(source, tilt)
            DegreeReadouts(tilt)
            ZeroControls(tilt, zero, onZero = store::zeroHere, onClear = store::clearZero)
        }
    }
}

/** The instrument itself — square, drawn behind, draggable on desktop. */
@Composable
private fun LevelSquare(source: house.level.data.TiltSource, tilt: Tilt?) {
    // Colors resolved here (per composition), passed into the DrawScope as
    // plain values — the ForeEdge pattern from the design kit.
    val colors = LevelColors(
        trackFill = MaterialTheme.colorScheme.surfaceVariant,
        trackStroke = MaterialTheme.colorScheme.outline,
        bubble = MaterialTheme.colorScheme.primary,
        bubbleLevel = MaterialTheme.emberColor,
        ticks = MaterialTheme.hintColor,
        ticksLevel = MaterialTheme.emberColor,
    )
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .tiltSimulation { x, y, z -> source.simulateGravity(x, y, z) }
            .drawBehind { drawLevel(tilt, colors) },
    )
}

/** "PITCH 2.3°   ROLL -1.7°" — the letterspaced reference voice. */
@Composable
private fun DegreeReadouts(tilt: Tilt?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DegreeLabel("PITCH", tilt?.pitchDeg, tilt?.let { isLevel(Tilt(it.pitchDeg, 0.0)) } ?: false)
        DegreeLabel("ROLL", tilt?.rollDeg, tilt?.let { isLevel(Tilt(0.0, it.rollDeg)) } ?: false)
    }
}

@Composable
private fun DegreeLabel(axis: String, degrees: Double?, level: Boolean) {
    Text(
        text = if (degrees == null) "$axis —" else "$axis ${formatDegrees(degrees)}°",
        style = MaterialTheme.reference,
        color = if (level) MaterialTheme.emberColor else MaterialTheme.hintColor,
    )
}

/** One button at a time: "Zero here" until zeroed, then "Clear zero". */
@Composable
private fun ZeroControls(tilt: Tilt?, zero: Tilt?, onZero: () -> Unit, onClear: () -> Unit) {
    if (zero == null) {
        PrimaryButton(text = "Zero here", onClick = onZero, enabled = tilt != null)
    } else {
        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
            Text("Clear zero", style = MaterialTheme.reference)
        }
    }
}