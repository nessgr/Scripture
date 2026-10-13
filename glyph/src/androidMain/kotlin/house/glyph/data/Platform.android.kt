package house.glyph.data

import java.io.File

/** Plain files in the app's private storage — nothing ever leaves the device. */
actual fun defaultDataStorage(): DataStorage = object : DataStorage {
    private val dir = GlyphAndroidApp.context.filesDir

    override fun read(name: String): String? {
        val file = File(dir, name)
        return if (file.exists()) file.readText() else null
    }

    override fun write(name: String, content: String) {
        File(dir, name).writeText(content)
    }
}