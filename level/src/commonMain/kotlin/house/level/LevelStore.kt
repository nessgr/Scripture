package house.level

import house.level.data.TiltSource
import house.level.domain.Tilt
import house.level.domain.applyZero
import house.level.domain.tiltFromGravity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * The level's whole state: gravity in, calibrated tilt out. [tilt] is
 * `null` until the first sample arrives (sensor-less device, or a desktop
 * before the first drag) — the UI's disabled state.
 *
 * [zeroHere] captures the *unzeroed* tilt as "flat", so the displayed tilt
 * is raw minus zero — set the phone on the wonky table, press Zero, and
 * the table reads 0.0°. The zero is in-memory only this round; persisting
 * it (a DataStorage blob, ThemeStore-style) is deliberate future work —
 * a level remembers its calibration across launches once it earns it.
 */
class LevelStore(private val source: TiltSource) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _zero = MutableStateFlow<Tilt?>(null)
    val zero: StateFlow<Tilt?> = _zero.asStateFlow()

    val tilt: StateFlow<Tilt?> = combine(source.gravity, _zero) { gravity, zero ->
        gravity?.let { applyZero(tiltFromGravity(it), zero) }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** Capture the current orientation as flat. No-op before the first sample. */
    fun zeroHere() {
        _zero.value = source.gravity.value?.let(::tiltFromGravity)
    }

    /** Forget the calibration — trust gravity as ground truth again. */
    fun clearZero() {
        _zero.value = null
    }
}