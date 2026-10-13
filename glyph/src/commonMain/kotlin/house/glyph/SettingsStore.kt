package house.glyph

import house.design.ThemeChoice
import house.glyph.data.DataStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The game's remembered display preferences — theme and large text —
 * persisted as one JSON blob (ThemeStore style: read once at
 * construction, write-through on change, defaults for anything missing,
 * corrupt, or hand-edited).
 */
class SettingsStore(private val storage: DataStorage) {

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<GlyphSettings> = _settings.asStateFlow()

    val choice: ThemeChoice get() = ThemeChoice.entries.firstOrNull { it.name == _settings.value.choice } ?: ThemeChoice.SYSTEM
    val largeText: Boolean get() = _settings.value.largeText

    fun setChoice(choice: ThemeChoice) = update { it.copy(choice = choice.name) }

    fun setLargeText(largeText: Boolean) = update { it.copy(largeText = largeText) }

    /** Change, dedup, and write through — one blob, one file, atomically enough at this size. */
    private fun update(transform: (GlyphSettings) -> GlyphSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        storage.write(SETTINGS_BLOB, json.encodeToString(GlyphSettings.serializer(), next))
    }

    /** Missing, corrupt, or hand-edited files all fall back to the defaults. */
    private fun read(): GlyphSettings =
        storage.read(SETTINGS_BLOB)
            ?.let { text -> runCatching { json.decodeFromString<GlyphSettings>(text) }.getOrNull() }
            ?: GlyphSettings()

    companion object {
        private const val SETTINGS_BLOB = "glyph-settings.json"

        /** Shared with the other stores, which persist the same way. */
        internal val json = Json {
            ignoreUnknownKeys = true  // tolerate fields added by newer versions
            encodeDefaults = true     // defaults round-trip, so files are complete
            prettyPrint = true        // humans can peek at the file
        }
    }
}

/** The persisted settings document. Enums ride as names, decoded with fallback. */
@Serializable
data class GlyphSettings(
    val choice: String = ThemeChoice.SYSTEM.name,
    val largeText: Boolean = false,
)