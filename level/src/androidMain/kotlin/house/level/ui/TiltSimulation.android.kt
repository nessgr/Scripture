package house.level.ui

import androidx.compose.ui.Modifier

/** The real sensor drives the level — nothing to simulate. */
actual fun Modifier.tiltSimulation(onGravity: (x: Double, y: Double, z: Double) -> Unit): Modifier = this