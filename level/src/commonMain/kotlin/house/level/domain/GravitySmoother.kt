package house.level.domain

/**
 * Exponential moving average over the gravity vector, per component. The
 * noisy Android sensor and the desktop's discrete drag steps both flow
 * through one smoother so the bubble behaves identically on every platform.
 * [alpha] is the weight of the new sample: 1 = follow instantly, ~0.25 =
 * calm at game rate (a bubble that reads as liquid, not jittery).
 */
class GravitySmoother(private val alpha: Double) {

    private var value: Gravity? = null

    fun update(sample: Gravity): Gravity {
        val previous = value ?: sample.also { value = it }
        val next = Gravity(
            x = previous.x + alpha * (sample.x - previous.x),
            y = previous.y + alpha * (sample.y - previous.y),
            z = previous.z + alpha * (sample.z - previous.z),
        )
        value = next
        return next
    }
}