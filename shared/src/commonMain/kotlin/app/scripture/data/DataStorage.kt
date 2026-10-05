package app.scripture.data

/**
 * The entire persistence surface of the app: named text blobs.
 * Everything above this layer works only with strings, so the JSON
 * logic can be tested with a plain in-memory map.
 */
interface DataStorage {
    fun read(name: String): String?
    fun write(name: String, content: String)
}

/**
 * Where files physically live differs per platform (Android app dir vs.
 * desktop %APPDATA%), so each target supplies its own implementation.
 */
expect fun defaultDataStorage(): DataStorage