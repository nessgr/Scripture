package house.level.data

/**
 * The entire persistence surface of the app: named text blobs.
 * Everything above this layer works only with strings, so the JSON
 * logic can be tested with a plain in-memory map. (The same pattern as
 * :shared's app.scripture.data.DataStorage — Level can't reuse that one,
 * it lives in a module Level doesn't depend on.)
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