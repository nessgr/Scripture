package house.level.data

import java.io.File

/**
 * Android storage: plain files in the app's private files dir. MainActivity
 * registers the application context once at startup (LevelAndroidApp), the
 * same seam the tilt source uses.
 */
actual fun defaultDataStorage(): DataStorage = object : DataStorage {
    private val dir = LevelAndroidApp.context.filesDir

    override fun read(name: String): String? {
        val file = File(dir, name)
        return if (file.exists()) file.readText() else null
    }

    override fun write(name: String, content: String) {
        File(dir, name).writeText(content)
    }
}