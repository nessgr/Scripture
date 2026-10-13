package house.glyph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsStoreTest {

    @Test
    fun `an empty storage gives a clean slate`() {
        val stats = StatsStore(InMemoryDataStorage()).stats.value
        assertEquals(0, stats.played)
        assertEquals(0, stats.wins)
        assertEquals(0, stats.streak)
        assertEquals(0, stats.bestStreak)
        assertEquals(List(6) { 0 }, stats.distribution)
    }

    @Test
    fun `a win extends the streak and fills its slot in the distribution`() {
        val store = StatsStore(InMemoryDataStorage())
        store.recordWin(3)
        val stats = store.stats.value
        assertEquals(1, stats.played)
        assertEquals(1, stats.wins)
        assertEquals(1, stats.streak)
        assertEquals(1, stats.bestStreak)
        assertEquals(listOf(0, 0, 1, 0, 0, 0), stats.distribution)

        store.recordWin(1)
        assertEquals(2, store.stats.value.wins)
        assertEquals(2, store.stats.value.streak)
        assertEquals(listOf(1, 0, 1, 0, 0, 0), store.stats.value.distribution)
    }

    @Test
    fun `a loss resets the streak but keeps the best`() {
        val store = StatsStore(InMemoryDataStorage())
        store.recordWin(2)
        store.recordWin(4)
        store.recordLoss()
        val stats = store.stats.value
        assertEquals(3, stats.played)
        assertEquals(2, stats.wins)
        assertEquals(0, stats.streak)
        assertEquals(2, stats.bestStreak)
    }

    @Test
    fun `stats survive a new store over the same storage`() {
        val storage = InMemoryDataStorage()
        StatsStore(storage).apply {
            recordWin(2)
            recordLoss()
        }

        val resumed = StatsStore(storage).stats.value
        assertEquals(2, resumed.played)
        assertEquals(1, resumed.wins)
        assertEquals(0, resumed.streak)
        assertEquals(1, resumed.bestStreak)
        assertEquals(listOf(0, 1, 0, 0, 0, 0), resumed.distribution)
    }

    @Test
    fun `a corrupt document means a clean slate`() {
        val storage = InMemoryDataStorage()
        storage.write("glyph-stats.json", "{not json")
        assertEquals(0, StatsStore(storage).stats.value.played)
    }

    @Test
    fun `a hand-edited distribution falls back to empty but keeps the counts`() {
        val storage = InMemoryDataStorage()
        storage.write("glyph-stats.json", """{"played":4,"wins":2,"streak":1,"bestStreak":3,"distribution":[1,2]}""")
        val stats = StatsStore(storage).stats.value
        assertEquals(4, stats.played)
        assertEquals(2, stats.wins)
        assertEquals(List(6) { 0 }, stats.distribution)
        assertTrue(stats.bestStreak == 3)
    }
}