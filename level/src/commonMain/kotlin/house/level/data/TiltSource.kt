package house.level.data

import house.level.domain.Gravity
import kotlinx.coroutines.flow.StateFlow

/**
 * The platform's gravity reading, smoothed; `null` until the first sample
 * arrives (a sensor-less device, or a desktop before its first drag).
 */
interface TiltSource {
    val gravity: StateFlow<Gravity?>

    /**
     * Desktop-only hook: hand a gravity vector to the level as if the
     * device had been tilted. The JVM actual implements it; Android
     * inherits this no-op so common UI code never learns which platform
     * it is on.
     */
    fun simulateGravity(x: Double, y: Double, z: Double) {}
}

/** Platform tilt source: SensorManager on Android, drag simulation on desktop. */
expect fun defaultTiltSource(): TiltSource