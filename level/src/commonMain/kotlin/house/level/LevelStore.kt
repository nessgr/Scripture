package house.level

import house.level.data.DataStorage
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
 * the table reads 0.0°. The zero persists as its own blob (settings.json
 * writes never interleave with it), written synchronously the ThemeStore
 * way: a few bytes on an explicit action. A missing, blank, or corrupt
 * file means no zero — gravity is ground truth again.
 */
class LevelStore(
    private val source: TiltSource,
    private val storage: DataStorage,
) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _zero = MutableStateFlow(readZero())
    val zero: StateFlow<Tilt?> = _zero.asStateFlow()

    val tilt: StateFlow<Tilt?> = combine(source.gravity, _zero) { gravity, zero ->
        gravity?.let { applyZero(tiltFromGravity(it), zero) }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** Capture the current orientation as flat. No-op before the first sample. */
    fun zeroHere() {
        writeZero(source.gravity.value?.let(::tiltFromGravity))
    }

    /** Forget the calibration — trust gravity as ground truth again. */
    fun clearZero() {
        writeZero(null)
    }

    /** Blank file = no zero, so clearing and never-zeroed read the same. */
    private fun writeZero(zero: Tilt?) {
        _zero.value = zero
        val content = zero?.let { SettingsStore.json.encodeToString(ZeroData.serializer(), ZeroData(it.pitchDeg, it.rollDeg)) } ?: ""
        storage.write(ZERO_BLOB, content)
    }

    /** The persisted zero, or null for missing/blank/corrupt. */
    private fun readZero(): Tilt? {
        val text = storage.read(ZERO_BLOB) ?: return null
        if (text.isBlank()) return null
        val data = runCatching { SettingsStore.json.decodeFromString<ZeroData>(text) }.getOrNull() ?: return null
        return Tilt(pitchDeg = data.pitchDeg, rollDeg = data.rollDeg)
    }

    private companion object {
        const val ZERO_BLOB = "level-zero.json"
    }
}