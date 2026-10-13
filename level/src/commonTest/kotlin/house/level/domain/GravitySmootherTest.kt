package house.level.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GravitySmootherTest {

    @Test
    fun `the first sample passes straight through`() {
        val smoother = GravitySmoother(alpha = 0.25)
        val first = Gravity(1.0, -2.0, 9.0)
        assertEquals(first, smoother.update(first))
    }

    @Test
    fun `alpha of one follows the input immediately`() {
        val smoother = GravitySmoother(alpha = 1.0)
        smoother.update(Gravity(0.0, 0.0, 9.81))
        val next = Gravity(3.0, 4.0, 5.0)
        assertEquals(next, smoother.update(next))
    }

    @Test
    fun `alpha of one half steps halfway to each new sample`() {
        val smoother = GravitySmoother(alpha = 0.5)
        smoother.update(Gravity(0.0, 0.0, 9.81))
        val output = smoother.update(Gravity(2.0, -4.0, 9.81))
        assertEquals(1.0, output.x, 1e-9)
        assertEquals(-2.0, output.y, 1e-9)
        assertEquals(9.81, output.z, 1e-9)
    }

    @Test
    fun `a fixed input converges to itself`() {
        val smoother = GravitySmoother(alpha = 0.25)
        val target = Gravity(0.5, 0.5, 9.81)
        repeat(50) { smoother.update(target) }
        assertEquals(target, smoother.update(target))
    }
}