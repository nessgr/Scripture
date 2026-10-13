package house.level.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sqrt

/*
 * Desktop tilt simulation. A virtual gravity vector starts flat
 * (0, 0, 1); dragging nudges gx/gy and renormalizes back to unit length so
 * the math path is the same one a real accelerometer feeds. Drag down
 * tilts the top edge down, matching the device convention (y toward the
 * top of the screen). Double-tap resets to flat — one gesture back to a
 * known state while iterating on the UI.
 */
actual fun Modifier.tiltSimulation(onGravity: (x: Double, y: Double, z: Double) -> Unit): Modifier {
    var gx = 0.0
    var gy = 0.0
    val dragSensitivity = 300.0 // pixels of drag per unit of gravity nudge

    fun report() {
        val z = sqrt((1.0 - gx * gx - gy * gy).coerceAtLeast(0.0))
        onGravity(gx, gy, z)
    }

    return this
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = {
                gx = 0.0; gy = 0.0
                report()
            })
        }
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                gx = (gx + dragAmount.x / dragSensitivity).coerceIn(-0.99, 0.99)
                gy = (gy + dragAmount.y / dragSensitivity).coerceIn(-0.99, 0.99)
                report()
            }
        }
}