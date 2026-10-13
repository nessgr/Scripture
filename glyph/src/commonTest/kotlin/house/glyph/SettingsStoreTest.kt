package house.glyph

import house.design.ThemeChoice
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
    }

    @Test
    fun `settings survive a new store over the same storage`() {
        val storage = InMemoryDataStorage()
        val first = SettingsStore(storage)
        first.setChoice(ThemeChoice.DARK)
        first.setLargeText(true)

        val second = SettingsStore(storage)
        assertEquals(ThemeChoice.DARK, second.choice)
        assertTrue(second.largeText)
    }

    @Test
    fun `a corrupt document falls back to the defaults`() {
        val storage = InMemoryDataStorage()
        storage.write("glyph-settings.json", "{not json at all")
        val store = SettingsStore(storage)
        assertEquals(ThemeChoice.SYSTEM, store.choice)
        assertFalse(store.largeText)
    }

    @Test
    fun `an unknown theme name falls back to auto`() {
        val storage = InMemoryDataStorage()
        storage.write("glyph-settings.json", """{"choice":"NEON","largeText":true}""")
        val store = SettingsStore(storage)
        assertEquals(ThemeChoice.SYSTEM, store.choice)
        assertTrue(store.largeText) // the rest of the document survives
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
        store.setLargeText(false) // already the default
        assertEquals(writesBefore, storage.writes)
    }
}