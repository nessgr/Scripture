package house.glyph.data

import android.content.Context

/**
 * The application context, registered once by the Android entry point
 * before any storage is constructed — the same registration shape as
 * :level's LevelAndroidApp.
 */
object GlyphAndroidApp {
    lateinit var context: Context
}