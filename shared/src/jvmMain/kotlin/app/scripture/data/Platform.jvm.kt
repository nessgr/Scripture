package app.scripture.data

import java.io.File

/**
 * Desktop storage: a `Scripture` folder under %APPDATA% —
 * the standard Windows location for per-user app data.
 */
actual fun defaultDataStorage(): DataStorage = object : DataStorage {
    private val dir = File(System.getProperty("user.home"), "AppData/Local/Scripture")
        .apply { mkdirs() }

    override fun read(name: String): String? {
        val file = File(dir, name)
        return if (file.exists()) file.readText() else null
    }

    override fun write(name: String, content: String) {
        File(dir, name).writeText(content)
    }
}