package house.level.data

import house.level.domain.Gravity
import house.level.domain.GravitySmoother
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop tilt source: there is no accelerometer, so the UI's drag
 * simulation feeds gravity in via [simulateGravity] — through the same
 * smoother the Android sensor uses, so the bubble behaves identically.
 */
private class JvmTiltSource : TiltSource {

    private val smoother = GravitySmoother(alpha = 0.25)

    private val _gravity = MutableStateFlow<Gravity?>(null)
    override val gravity: StateFlow<Gravity?> = _gravity.asStateFlow()

    override fun simulateGravity(x: Double, y: Double, z: Double) {
        _gravity.value = smoother.update(Gravity(x, y, z))
    }
}

actual fun defaultTiltSource(): TiltSource = JvmTiltSource()