package house.level

import house.design.ThemeChoice
import house.level.domain.LengthUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsStoreTest {

    @Test
    fun `an empty storage gives the defaults`() {
        val store = SettingsStore(InMemoryDataStorage())
        assertEquals(ThemeChoice.SYSTEM, store.choice)
        assertFalse(store.largeText)
        assertEquals("0", store.runText)
        assertEquals(LengthUnits.FEET, store.units)
    }

    @Test
    fun `settings survive a new store over the same storage`() {
        val storage = InMemoryDataStorage()
        val first = SettingsStore(storage)
        first.setChoice(ThemeChoice.DARK)
        first.setLargeText(true)
        first.setRunText("7.5")
        first.setUnits(LengthUnits.METERS)

        val second = SettingsStore(storage)
        assertEquals(ThemeChoice.DARK, second.choice)
        assertTrue(second.largeText)
        assertEquals("7.5", second.runText)
        assertEquals(LengthUnits.METERS, second.units)
    }

    @Test
    fun `a corrupt document falls back to the defaults`() {
        val storage = InMemoryDataStorage()
        storage.write("level-settings.json", "{not json at all")
        val store = SettingsStore(storage)
        assertEquals(ThemeChoice.SYSTEM, store.choice)
        assertEquals("0", store.runText)
    }

    @Test
    fun `unknown enum names fall back to their defaults`() {
        val storage = InMemoryDataStorage()
        storage.write("level-settings.json", """{"choice":"NEON","units":"CUBITS"}""")
        val store = SettingsStore(storage)
        assertEquals(ThemeChoice.SYSTEM, store.choice)
        assertEquals(LengthUnits.FEET, store.units)
    }

    @Test
    fun `an unparseable run falls back to zero without losing the rest`() {
        val storage = InMemoryDataStorage()
        storage.write("level-settings.json", """{"choice":"DARK","runText":"abc"}""")
        val store = SettingsStore(storage)
        assertEquals("0", store.runText)
        assertEquals(ThemeChoice.DARK, store.choice)
    }

    @Test
    fun `setting the same value writes nothing`() {
        val storage = object : InMemoryDataStorage() {
            var writes = 0
            override fun write(name: String, content: String) {
                writes++
                super.write(name, content)
            }
        }
        val store = SettingsStore(storage)
        val writesBefore = storage.writes
        store.setChoice(ThemeChoice.SYSTEM) // already the default
        assertEquals(writesBefore, storage.writes)
    }
}