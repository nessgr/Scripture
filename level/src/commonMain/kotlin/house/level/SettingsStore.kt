package house.level

import house.design.ThemeChoice
import house.level.data.DataStorage
import house.level.domain.LengthUnits
import house.level.domain.runFromText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The level's remembered preferences — theme, large text, the run and its
 * units — persisted as one JSON blob (ThemeStore style: read once at
 * construction, write-through on change, defaults for anything missing,
 * corrupt, or hand-edited). Synchronous by design: a few bytes on explicit
 * user actions, no interleaving, trivially testable.
 */
class SettingsStore(private val storage: DataStorage) {

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<LevelSettings> = _settings.asStateFlow()

    val choice: ThemeChoice get() = ThemeChoice.entries.firstOrNull { it.name == _settings.value.choice } ?: ThemeChoice.SYSTEM
    val largeText: Boolean get() = _settings.value.largeText
    val runText: String get() = _settings.value.runText
    val units: LengthUnits get() = LengthUnits.entries.firstOrNull { it.name == _settings.value.units } ?: LengthUnits.FEET

    fun setChoice(choice: ThemeChoice) = update { it.copy(choice = choice.name) }

    fun setLargeText(largeText: Boolean) = update { it.copy(largeText = largeText) }

    fun setRunText(runText: String) = update { it.copy(runText = runText) }

    fun setUnits(units: LengthUnits) = update { it.copy(units = units.name) }

    /** Change, dedup, and write through — one blob, one file, atomically enough at this size. */
    private fun update(transform: (LevelSettings) -> LevelSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        storage.write(SETTINGS_BLOB, json.encodeToString(LevelSettings.serializer(), next))
    }

    /** Missing, corrupt, or hand-edited files all fall back to the defaults. */
    private fun read(): LevelSettings {
        val stored = storage.read(SETTINGS_BLOB)
            ?.let { text -> runCatching { json.decodeFromString<LevelSettings>(text) }.getOrNull() }
            ?: return LevelSettings()
        // A run that no longer parses (hand-edited file, old build) is as good as none.
        val run = stored.runText.takeIf { runFromText(it) != null } ?: "0"
        return stored.copy(runText = run)
    }

    companion object {
        private const val SETTINGS_BLOB = "level-settings.json"

        /** Shared with [LevelStore], which persists the zero the same way. */
        internal val json = Json {
            ignoreUnknownKeys = true  // tolerate fields added by newer versions
            encodeDefaults = true     // defaults round-trip, so files are complete
            prettyPrint = true         // humans can peek at the file
        }
    }
}

/** The persisted settings document. Enums ride as names, decoded with fallback. */
@Serializable
data class LevelSettings(
    val choice: String = ThemeChoice.SYSTEM.name,
    val largeText: Boolean = false,
    val runText: String = "0",
    val units: String = LengthUnits.FEET.name,
)

/** The zero calibration as persisted by [LevelStore]. */
@Serializable
data class ZeroData(
    val pitchDeg: Double,
    val rollDeg: Double,
)