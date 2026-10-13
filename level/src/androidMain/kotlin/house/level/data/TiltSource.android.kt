package house.level.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import house.level.domain.Gravity
import house.level.domain.GravitySmoother
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * The one piece of the level that is Android: turn sensor events into a
 * smooth gravity StateFlow. Registration happens at construction and is
 * deliberately never released — the app has one screen and one activity,
 * so the listener lives as long as the process, exactly like
 * ScriptureStore's scope. (If Level ever grows navigation or multiple
 * entry points, revisit with lifecycle-aware registration.)
 */
actual fun defaultTiltSource(): TiltSource = AndroidTiltSource(LevelAndroidApp.context)

private class AndroidTiltSource(context: Context) : TiltSource {

    private val smoother = GravitySmoother(alpha = 0.25)

    private val _gravity = MutableStateFlow<Gravity?>(null)
    override val gravity: StateFlow<Gravity?> = _gravity.asStateFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // Values arrive in device axes (x right, y top, z out of the glass).
            _gravity.value = smoother.update(Gravity(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble()))
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    init {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Prefer the vendor-fused gravity sensor — already low-passed, so the
        // bubble reads as liquid rather than jittery. Emulators often lack it;
        // the raw accelerometer (linear motion included) is the fallback.
        val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        // sensor == null leaves gravity at null forever — the UI's disabled
        // state. Manifest declares the accelerometer feature required=false.
    }
}