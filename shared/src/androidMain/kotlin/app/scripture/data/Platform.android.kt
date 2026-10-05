package app.scripture.data

import android.content.Context

/**
 * Tiny bridge: MainActivity registers the application context once at
 * startup; nothing else in the app knows Android exists.
 */
object AndroidApp {
    lateinit var context: Context
}

/**
 * Android storage: the app's private files dir — wiped if the app is
 * uninstalled, invisible to other apps, no permissions needed.
 */
actual fun defaultDataStorage(): DataStorage = object : DataStorage {
    private val dir = AndroidApp.context.filesDir

    override fun read(name: String): String? {
        val file = java.io.File(dir, name)
        return if (file.exists()) file.readText() else null
    }

    override fun write(name: String, content: String) {
        java.io.File(dir, name).writeText(content)
    }
}