package house.level

import house.level.data.DataStorage

/** In-memory DataStorage for tests: persistence without the platform files. */
open class InMemoryDataStorage : DataStorage {
    private val files = mutableMapOf<String, String>()
    override fun read(name: String): String? = files[name]
    override fun write(name: String, content: String) { files[name] = content }
}