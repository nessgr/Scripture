package house.level.data

import java.io.File

/**
 * Desktop storage: plain files under %USERPROFILE%\AppData\Local\Level
 * (mirroring :shared's AppData\Local\Scripture). Blocking, synchronous —
 * callers keep writes to a few bytes on explicit user actions.
 */
actual fun defaultDataStorage(): DataStorage = object : DataStorage {
    private val dir = File(System.getProperty("user.home"), "AppData/Local/Level")
        .apply { mkdirs() }

    override fun read(name: String): String? {
        val file = File(dir, name)
        return if (file.exists()) file.readText() else null
    }

    override fun write(name: String, content: String) {
        File(dir, name).writeText(content)
    }
}