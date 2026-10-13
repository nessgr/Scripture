package house.glyph

import house.glyph.data.DataStorage
import house.glyph.domain.MAX_GUESSES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * The player's record — played, wins, streaks, and how many guesses
 * wins take — persisted as one JSON blob. Wins extend the streak,
 * losses reset it; a hand-edited or corrupt file means a clean slate
 * rather than a broken game.
 */
class StatsStore(private val storage: DataStorage) {

    private val _stats = MutableStateFlow(read())
    val stats: StateFlow<GlyphStats> = _stats.asStateFlow()

    /** Called once, by the game store, when a round ends. */
    fun recordWin(guessCount: Int) = update {
        val streak = it.streak + 1
        it.copy(
            played = it.played + 1,
            wins = it.wins + 1,
            streak = streak,
            bestStreak = maxOf(it.bestStreak, streak),
            distribution = it.distribution.toMutableList().also { list ->
                val slot = guessCount.coerceIn(1, MAX_GUESSES)
                list[slot - 1] = list[slot - 1] + 1
            },
        )
    }

    fun recordLoss() = update {
        it.copy(played = it.played + 1, streak = 0)
    }

    private fun update(transform: (GlyphStats) -> GlyphStats) {
        val next = transform(_stats.value)
        if (next == _stats.value) return
        _stats.value = next
        storage.write(STATS_BLOB, SettingsStore.json.encodeToString(GlyphStats.serializer(), next))
    }

    private fun read(): GlyphStats =
        storage.read(STATS_BLOB)
            ?.let { text -> runCatching { SettingsStore.json.decodeFromString<GlyphStats>(text) }.getOrNull() }
            ?.let { stored ->
                // A distribution the game didn't write (hand-edited, older build) is as good as none.
                stored.copy(distribution = stored.distribution.takeIf { it.size == MAX_GUESSES } ?: GlyphStats().distribution)
            }
            ?: GlyphStats()

    companion object {
        private const val STATS_BLOB = "glyph-stats.json"
    }
}

/** The persisted record. */
@Serializable
data class GlyphStats(
    val played: Int = 0,
    val wins: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val distribution: List<Int> = List(MAX_GUESSES) { 0 },
)