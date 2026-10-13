package house.level.domain

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TiltTest {

    @Test
    fun `flat face-up gravity is perfectly level`() {
        val tilt = tiltFromGravity(Gravity(0.0, 0.0, 9.81))
        assertEquals(0.0, tilt.pitchDeg, 1e-9)
        assertEquals(0.0, tilt.rollDeg, 1e-9)
        assertTrue(isLevel(tilt))
    }

    @Test
    fun `equal x and z gravity means 45 degrees of roll`() {
        val tilt = tiltFromGravity(Gravity(9.81, 0.0, 9.81))
        assertEquals(45.0, tilt.rollDeg, 1e-9)
        assertEquals(0.0, tilt.pitchDeg, 1e-9)
    }

    @Test
    fun `positive y gravity means the top edge is down`() {
        val tilt = tiltFromGravity(Gravity(0.0, 5.0, 9.81))
        assertTrue(tilt.pitchDeg > 0)
        assertEquals(0.0, tilt.rollDeg, 1e-9)
    }

    @Test
    fun `positive x gravity means the right edge is down`() {
        val tilt = tiltFromGravity(Gravity(5.0, 0.0, 9.81))
        assertTrue(tilt.rollDeg > 0)
        assertEquals(0.0, tilt.pitchDeg, 1e-9)
    }

    @Test
    fun `applyZero subtracts the captured zero per component`() {
        val raw = Tilt(pitchDeg = 3.0, rollDeg = -2.0)
        val zeroed = applyZero(raw, Tilt(pitchDeg = 1.0, rollDeg = 2.0))
        assertEquals(2.0, zeroed.pitchDeg, 1e-9)
        assertEquals(-4.0, zeroed.rollDeg, 1e-9)
    }

    @Test
    fun `applyZero with no zero is the identity`() {
        val raw = Tilt(pitchDeg = 3.0, rollDeg = -2.0)
        assertEquals(raw, applyZero(raw, null))
    }

    @Test
    fun `isLevel is true exactly at the tolerance boundary`() {
        val boundary = Tilt(pitchDeg = 0.5, rollDeg = -0.5)
        assertTrue(isLevel(boundary))
        assertFalse(isLevel(boundary, toleranceDeg = 0.4))
        assertFalse(isLevel(Tilt(0.0, 0.6)))
        assertFalse(isLevel(Tilt(0.6, 0.0)))
    }

    @Test
    fun `bubbleFraction follows sign, full scale, and clamps past it`() {
        assertEquals(0.5, bubbleFraction(4.0), 1e-9) // half of 8° full scale
        assertEquals(-0.25, bubbleFraction(-2.0), 1e-9)
        assertEquals(1.0, bubbleFraction(20.0), 1e-9)  // clamped
        assertEquals(-1.0, bubbleFraction(-20.0), 1e-9)
        assertEquals(0.25, bubbleFraction(1.0, fullScaleDeg = 4.0), 1e-9)
    }

    @Test
    fun `tiltMagnitude is the hypotenuse of the two axes`() {
        assertEquals(0.0, tiltMagnitudeDeg(Tilt(0.0, 0.0)), 1e-9)
        assertEquals(5.0, tiltMagnitudeDeg(Tilt(3.0, 4.0)), 1e-9)
        assertEquals(sqrt(2.0), tiltMagnitudeDeg(Tilt(1.0, 1.0)), 1e-9)
    }

    @Test
    fun `formatOneDecimal rounds to one decimal`() {
        assertEquals("0.0", formatOneDecimal(0.0))
        assertEquals("2.3", formatOneDecimal(2.34))
        assertEquals("13.0", formatOneDecimal(13.04))
        assertEquals("2.4", formatOneDecimal(2.35))
        assertEquals("-7.1", formatOneDecimal(-7.14))
    }

    @Test
    fun `formatOneDecimal never prints negative zero`() {
        assertEquals("0.0", formatOneDecimal(-0.04))
        assertEquals("-0.1", formatOneDecimal(-0.06)) // a real negative value keeps its sign
    }
}