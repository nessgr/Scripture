package house.level.ui

import androidx.compose.ui.Modifier

/**
 * Desktop: drag inside the level to simulate tilting the device; double-tap
 * resets flat. Android: a no-op — the real sensor drives the level, and this
 * must not eat pointer events.
 */
expect fun Modifier.tiltSimulation(onGravity: (x: Double, y: Double, z: Double) -> Unit): Modifier