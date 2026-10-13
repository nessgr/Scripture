package house.level.data

import android.content.Context

/**
 * The application context, registered once by the Android entry point
 * before any TiltSource is constructed — the same registration shape as
 * :shared's AndroidApp (Level can't reuse that one; it lives in :shared,
 * which this module doesn't depend on).
 */
object LevelAndroidApp {
    lateinit var context: Context
}