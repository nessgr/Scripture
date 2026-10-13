package app.scripture

import app.scripture.data.DataStorage
import house.design.ThemeChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds and persists [ThemeChoice] as its own named blob — deliberately
 * separate from the verse document, so a theme write can never interleave
 * with library persistence or corrupt it. Reads once at construction and
 * writes once per tap: a few bytes on an explicit user action, written
 * synchronously (unlike the verse document, this one is too small to
 * ever jank, and determinism makes it testable).
 */
class ThemeStore(private val storage: DataStorage) {

    private val _choice = MutableStateFlow(read())
    val choice: StateFlow<ThemeChoice> = _choice.asStateFlow()

    fun set(choice: ThemeChoice) {
        if (_choice.value == choice) return
        _choice.value = choice
        storage.write(BLOB, choice.name)
    }

    /** Unknown values (a hand-edited file, an old build) fall back to SYSTEM. */
    private fun read(): ThemeChoice {
        val stored = storage.read(BLOB) ?: return ThemeChoice.SYSTEM
        return ThemeChoice.entries.firstOrNull { it.name == stored } ?: ThemeChoice.SYSTEM
    }

    private companion object {
        const val BLOB = "theme"
    }
}