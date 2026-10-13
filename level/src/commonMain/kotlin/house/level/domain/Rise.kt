package house.level.domain

import kotlin.math.PI
import kotlin.math.tan

/*
 * The rise-over-run calculator: run in, rise out. Pure like everything else
 * in this package — the camper use case (tires 7 ft apart, "how much lift
 * on the low side?") is the test vector below.
 */

/**
 * Length system for the calculator. [factor] converts the run unit into
 * the smaller unit the rise is shown in: feet → inches (12), meters →
 * centimeters (100) — a rise of "0.2 ft" or "0.03 m" is useless under a
 * camper; inches and centimeters are the working numbers.
 */
enum class LengthUnits(val runLabel: String, val riseLabel: String, val factor: Double) {
    FEET("ft", "in", 12.0),
    METERS("m", "cm", 100.0),
}

/**
 * Rise over a run at an angle: how far the low side must be lifted,
 * `run × tan(angle)` in the rise unit of [units] (the run unit scaled by
 * its [LengthUnits.factor]). The angle comes from the calibrated tilt, so
 * a zeroed level reads a zero rise. tan explodes toward ±90°, but a spirit
 * level never gets there — no clamp; the caller's tilt is already bounded.
 */
fun riseLength(run: Double, angleDeg: Double, units: LengthUnits): Double =
    run * units.factor * tan(angleDeg * PI / 180.0)

/**
 * The run as typed into the field, parsed defensively: null for blank,
 * non-numeric, or negative text. The UI also sanitizes keystrokes; this is
 * the parser of record.
 */
fun runFromText(text: String): Double? =
    text.toDoubleOrNull()?.takeIf { it >= 0.0 }