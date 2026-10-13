package house.level.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import house.design.PrimaryButton
import house.design.ScreenScaffold
import house.design.Spacing
import house.design.TagChip
import house.design.ThemeToggle
import house.design.emberColor
import house.design.hintColor
import house.design.inkColor
import house.design.readingDisplay
import house.design.reference
import house.level.LevelStore
import house.level.SettingsStore
import house.level.data.DataStorage
import house.level.data.defaultTiltSource
import house.level.domain.LengthUnits
import house.level.domain.Tilt
import house.level.domain.formatOneDecimal
import house.level.domain.isLevel
import house.level.domain.riseLength
import house.level.domain.runFromText

/**
 * The instrument screen: the level square (vials engraved with degrees
 * and rise) and the calculator's control row. Tilt arrives from the store;
 * drawing stays in LevelDrawing.kt, controls live here. The remembered
 * preferences (theme, large text, run, units) flow in from [settings];
 * [storage] lets the level store persist its zero. [onHelp] opens the
 * help page. Large text (the house accessibility mode) swaps the vials'
 * rise line for a big degrees-and-rise bar.
 */
@Composable
fun LevelScreen(settings: SettingsStore, storage: DataStorage, onHelp: () -> Unit) {
    val source = remember { defaultTiltSource() }
    val store = remember { LevelStore(source, storage) }
    val tilt by store.tilt.collectAsState()
    val zero by store.zero.collectAsState()
    val current by settings.settings.collectAsState()
    val run = runFromText(current.runText)

    ScreenScaffold(
        title = "Level",
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(onClick = onHelp) {
                    Text("Help", style = MaterialTheme.reference)
                }
                ThemeToggle(choice = settings.choice, onChoice = { settings.setChoice(it) })
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            LevelSquare(source, tilt, run, settings.units, current.largeText)
            if (current.largeText) BigReadouts(tilt, run, settings.units)
            RiseSection(
                tilt = tilt,
                zero = zero,
                runText = current.runText,
                onRunText = { text -> settings.setRunText(text.filter { it.isDigit() || it == '.' }) },
                units = settings.units,
                onUnits = settings::setUnits,
                onZero = store::zeroHere,
                onClear = store::clearZero,
            )
        }
    }
}

/** The instrument itself — square, drawn behind, draggable on desktop. */
@Composable
private fun LevelSquare(source: house.level.data.TiltSource, tilt: Tilt?, run: Double?, units: LengthUnits, largeText: Boolean) {
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
    // The readings engraved in the vials — degrees, plus the rise once a
    // run is entered. Large text hands the rise line to the big readout
    // (two grown lines no longer fit the glass) and speaks louder: ink
    // instead of the quiet hint.
    val readouts = LevelReadouts(
        pitch = vialReading(tilt?.pitchDeg, run, units, includeRise = !largeText),
        roll = vialReading(tilt?.rollDeg, run, units, includeRise = !largeText),
        style = MaterialTheme.reference,
        pitchColor = engravingColor(tilt?.let { isLevel(Tilt(it.pitchDeg, 0.0)) } == true, largeText),
        rollColor = engravingColor(tilt?.let { isLevel(Tilt(0.0, it.rollDeg)) } == true, largeText),
    )
    val textMeasurer = rememberTextMeasurer()
    // The draw block reads these as *state*, not captures: DrawBehind's node
    // update doesn't invalidate the drawing on its own, so captured values
    // could go stale until something else redrew the box. Reading through
    // rememberUpdatedState subscribes the drawing to every change — drag,
    // run edit, unit switch — no recomposition tricks needed.
    val tiltState = rememberUpdatedState(tilt)
    val colorsState = rememberUpdatedState(colors)
    val readoutsState = rememberUpdatedState(readouts)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .tiltSimulation { x, y, z -> source.simulateGravity(x, y, z) }
            .drawBehind { drawLevel(tiltState.value, colorsState.value, readoutsState.value, textMeasurer) },
    )
}

/** Ember when level; hint normally, ink when large text asks for louder. */
@Composable
private fun engravingColor(level: Boolean, largeText: Boolean): Color =
    if (level) MaterialTheme.emberColor
    else if (largeText) MaterialTheme.inkColor
    else MaterialTheme.hintColor

/** What a vial says: "2.3°", growing to a second line — the rise — once a run exists. */
private fun vialReading(degrees: Double?, run: Double?, units: LengthUnits, includeRise: Boolean = true): String? {
    if (degrees == null) return null
    val rise = if (!includeRise) null
        else run?.let { formatOneDecimal(riseLength(it, degrees, units)) + " " + units.riseLabel }
    return formatOneDecimal(degrees) + "°" + if (rise == null) "" else "\n$rise"
}

/**
 * The big readout (large text only): degrees and rise in the display voice,
 * for numbers the glass can't grow big enough to hold. Ember says "level",
 * ink does the rest of the talking.
 */
@Composable
private fun BigReadouts(tilt: Tilt?, run: Double?, units: LengthUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        BigReadout(
            axis = "PITCH",
            degrees = tilt?.pitchDeg,
            level = tilt?.let { isLevel(Tilt(it.pitchDeg, 0.0)) } == true,
            run = run,
            units = units,
        )
        BigReadout(
            axis = "ROLL",
            degrees = tilt?.rollDeg,
            level = tilt?.let { isLevel(Tilt(0.0, it.rollDeg)) } == true,
            run = run,
            units = units,
        )
    }
}

@Composable
private fun BigReadout(axis: String, degrees: Double?, level: Boolean, run: Double?, units: LengthUnits) {
    val rise = if (degrees == null || run == null) null
        else formatOneDecimal(riseLength(run, degrees, units)) + " " + units.riseLabel
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(axis, style = MaterialTheme.reference, color = MaterialTheme.hintColor)
        Text(
            degrees?.let { formatOneDecimal(it) + "°" } ?: "—",
            style = MaterialTheme.readingDisplay,
            color = if (level) MaterialTheme.emberColor else MaterialTheme.inkColor,
        )
        Text(rise ?: "—", style = MaterialTheme.readingDisplay, color = MaterialTheme.inkColor)
    }
}

/**
 * The calculator's controls: the run field, its unit chips, and the zero
 * button, all on one line. The readings themselves — degrees and rise — are
 * engraved in the vials above; this row only feeds them. The rise follows
 * the zero calibration, so a zeroed reference lifts to zero.
 */
@Composable
private fun RiseSection(
    tilt: Tilt?,
    zero: Tilt?,
    runText: String,
    onRunText: (String) -> Unit,
    units: LengthUnits,
    onUnits: (LengthUnits) -> Unit,
    onZero: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("Run", style = MaterialTheme.reference, color = MaterialTheme.hintColor)
                OutlinedTextField(
                    value = runText,
                    onValueChange = onRunText,
                    placeholder = { Text("7") },
                    suffix = { Text(units.runLabel, style = MaterialTheme.reference) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.reference,
                    modifier = Modifier.width(140.dp),
                )
            }
            LengthUnits.entries.forEach { candidate ->
                TagChip(
                    label = candidate.runLabel,
                    selected = candidate == units,
                    onClick = { onUnits(candidate) },
                )
            }
            if (zero == null) {
                PrimaryButton(
                    text = "Zero here",
                    onClick = onZero,
                    enabled = tilt != null,
                    modifier = Modifier.weight(1f),
                )
            } else {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("Clear zero", style = MaterialTheme.reference)
                }
            }
        }
    }
}