package house.level

import house.level.data.TiltSource
import house.level.domain.Gravity
import house.level.domain.tiltFromGravity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertNull

/** A TiltSource with a settable sample — the sensor, for tests. */
private class FakeTiltSource : TiltSource {
    private val _gravity = MutableStateFlow<Gravity?>(null)
    override val gravity: StateFlow<Gravity?> = _gravity.asStateFlow()
    override fun simulateGravity(x: Double, y: Double, z: Double) {
        _gravity.value = Gravity(x, y, z)
    }
}

class LevelStoreTest {

    @Test
    fun `no storage, no zero`() {
        val store = LevelStore(FakeTiltSource(), InMemoryDataStorage())
        assertNull(store.zero.value)
    }

    @Test
    fun `a captured zero survives a new store over the same storage`() {
        val storage = InMemoryDataStorage()
        val source = FakeTiltSource()
        val first = LevelStore(source, storage)
        source.simulateGravity(0.1, 0.2, 9.81)
        first.zeroHere()
        val expected = tiltFromGravity(Gravity(0.1, 0.2, 9.81))

        val second = LevelStore(FakeTiltSource(), storage)
        assertEquals(expected, second.zero.value)
    }

    @Test
    fun `clearing the zero also clears the file`() {
        val storage = InMemoryDataStorage()
        val source = FakeTiltSource()
        val first = LevelStore(source, storage)
        source.simulateGravity(0.1, 0.2, 9.81)
        first.zeroHere()
        first.clearZero()

        val second = LevelStore(FakeTiltSource(), storage)
        assertNull(second.zero.value)
    }

    @Test
    fun `zeroHere is a no-op before the first sample`() {
        val store = LevelStore(FakeTiltSource(), InMemoryDataStorage())
        store.zeroHere()
        assertNull(store.zero.value)
    }

    @Test
    fun `a corrupt zero file means no zero`() {
        val storage = InMemoryDataStorage()
        storage.write("level-zero.json", "{not json")
        assertNull(LevelStore(FakeTiltSource(), storage).zero.value)
    }

    @Test
    fun `a blank zero file means no zero`() {
        val storage = InMemoryDataStorage()
        storage.write("level-zero.json", "")
        assertNull(LevelStore(FakeTiltSource(), storage).zero.value)
    }
}