package house.glyph.data

/**
 * Named text blobs on local disk — the same tiny seam as :shared's and
 * :level's (Glyph can't reuse theirs; those live in modules this one
 * doesn't depend on). All serialization happens above this layer, so the
 * implementations stay two methods each.
 */
interface DataStorage {
    fun read(name: String): String?

    fun write(name: String, content: String)
}

expect fun defaultDataStorage(): DataStorage