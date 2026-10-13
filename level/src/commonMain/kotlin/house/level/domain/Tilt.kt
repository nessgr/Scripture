package house.level.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToLong
import kotlin.math.sqrt

/*
 * The level's whole physics, pure and platform-free — everything here is
 * directly unit-tested. No Compose, no Android, no JVM.
 */

/**
 * The gravity vector in device axes (Android sensor convention, reused by
 * the desktop simulation so both platforms exercise one math path):
 * x → right edge of the screen, y → top of the screen, z → out of the glass.
 * Phone flat, face up: `Gravity(0, 0, +9.81)`.
 */
data class Gravity(val x: Double, val y: Double, val z: Double)

/**
 * Tilt in degrees. [rollDeg] is the rotation about the screen's y axis,
 * positive when the right edge is down; [pitchDeg] is the rotation about the
 * screen's x axis, positive when the top edge is down.
 */
data class Tilt(val pitchDeg: Double, val rollDeg: Double) {
    operator fun minus(other: Tilt) = Tilt(pitchDeg - other.pitchDeg, rollDeg - other.rollDeg)
}

/**
 * Gravity → tilt. The two atan2s are independent only near flat — fine for
 * a spirit level, which lives in that neighborhood; documented limitation.
 */
fun tiltFromGravity(g: Gravity): Tilt = Tilt(
    pitchDeg = toDegrees(atan2(g.y, g.z)),
    rollDeg = toDegrees(atan2(g.x, g.z)),
)

private fun toDegrees(radians: Double): Double = radians * 180.0 / kotlin.math.PI

/**
 * Tilt relative to a user-chosen "flat" [zero] — calibration. The zero holds
 * the *unzeroed* tilt captured when the button was pressed, so displayed
 * tilt is raw minus zero. `null` zero means "trust gravity as ground truth".
 */
fun applyZero(tilt: Tilt, zero: Tilt?): Tilt = zero?.let { tilt - it } ?: tilt

/** True when both axes sit within [toleranceDeg] of level. */
fun isLevel(tilt: Tilt, toleranceDeg: Double = 0.5): Boolean =
    abs(tilt.pitchDeg) <= toleranceDeg && abs(tilt.rollDeg) <= toleranceDeg

/** Overall how-far-from-flat, near-flat approximation of the angle. */
fun tiltMagnitudeDeg(tilt: Tilt): Double = sqrt(tilt.pitchDeg * tilt.pitchDeg + tilt.rollDeg * tilt.rollDeg)

/**
 * Where a bubble sits in its vial, as a fraction of its travel from center
 * (-1 … 1). [fullScaleDeg] is the tilt that sweeps the bubble to the end of
 * the vial — the vial's sensitivity.
 */
fun bubbleFraction(tiltDeg: Double, fullScaleDeg: Double = 8.0): Double =
    (tiltDeg / fullScaleDeg).coerceIn(-1.0, 1.0)

/**
 * One decimal place, e.g. "2.3". Hand-rolled rather than `"%.1f".format()`,
 * which is JVM-locale-dependent (decimal commas) and would make the common
 * tests platform-fragile. Never prints "-0.0".
 */
fun formatDegrees(v: Double): String {
    val rounded = (abs(v) * 10.0).roundToLong() / 10.0
    return (if (v < 0 && rounded > 0.0) "-" else "") + rounded.toString()
}