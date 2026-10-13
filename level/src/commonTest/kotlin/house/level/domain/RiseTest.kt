package house.level.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RiseTest {

    @Test
    fun `seven feet of run at two degrees rises about three inches`() {
        // The camper case the calculator exists for.
        assertEquals(2.9, riseLength(7.0, 2.0, LengthUnits.FEET), 0.05)
    }

    @Test
    fun `level means no rise`() {
        assertEquals(0.0, riseLength(7.0, 0.0, LengthUnits.FEET), 1e-9)
        assertEquals(0.0, riseLength(2.1, 0.0, LengthUnits.METERS), 1e-9)
    }

    @Test
    fun `at forty-five degrees rise equals run`() {
        // tan 45° = 1: a 1 ft run rises a whole 12 inches.
        assertEquals(12.0, riseLength(1.0, 45.0, LengthUnits.FEET), 1e-9)
    }

    @Test
    fun `meters run reports rise in centimeters`() {
        assertEquals(3.5, riseLength(1.0, 2.0, LengthUnits.METERS), 0.05)
    }

    @Test
    fun `each axis rises independently from the same run`() {
        val tilt = Tilt(pitchDeg = 2.0, rollDeg = -1.0)
        // Signed like the tilt itself: which side to lift follows the angle.
        assertEquals(-1.5, riseLength(7.0, tilt.rollDeg, LengthUnits.FEET), 0.05)
        assertEquals(2.9, riseLength(7.0, tilt.pitchDeg, LengthUnits.FEET), 0.05)
    }

    @Test
    fun `small angles are proportional to the run`() {
        val doubled = riseLength(14.0, 2.0, LengthUnits.FEET)
        assertEquals(2 * riseLength(7.0, 2.0, LengthUnits.FEET), doubled, 1e-9)
    }

    @Test
    fun `runFromText parses plain and decimal numbers`() {
        assertEquals(7.0, runFromText("7"))
        assertEquals(7.5, runFromText("7.5"))
        assertEquals(0.0, runFromText("0"))
    }

    @Test
    fun `runFromText rejects blank, garbage, and negative text`() {
        assertNull(runFromText(""))
        assertNull(runFromText("abc"))
        assertNull(runFromText("-1"))
        assertNull(runFromText("7.5.5"))
    }
}