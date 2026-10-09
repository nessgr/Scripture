package app.scripture

import app.scripture.data.InMemoryDataStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeStoreTest {

    @Test
    fun `defaults to the system choice when nothing is saved`() {
        val store = ThemeStore(InMemoryDataStorage())
        assertEquals(ThemeChoice.SYSTEM, store.choice.value)
    }

    @Test
    fun `a saved choice survives a new store over the same storage`() {
        val storage = InMemoryDataStorage()
        ThemeStore(storage).set(ThemeChoice.DARK)
        assertEquals(ThemeChoice.DARK, ThemeStore(storage).choice.value)
    }

    @Test
    fun `setting the same choice twice keeps it`() {
        val storage = InMemoryDataStorage()
        val store = ThemeStore(storage)
        store.set(ThemeChoice.LIGHT)
        store.set(ThemeChoice.LIGHT)
        assertEquals(ThemeChoice.LIGHT, store.choice.value)
    }

    @Test
    fun `an unrecognized saved value falls back to the system choice`() {
        val storage = InMemoryDataStorage()
        storage.write("theme", "neon")
        assertEquals(ThemeChoice.SYSTEM, ThemeStore(storage).choice.value)
    }
}