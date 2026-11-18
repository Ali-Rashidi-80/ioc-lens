package io.github.mrcoder20.ioclens.core.storage

import io.github.mrcoder20.ioclens.core.concurrency.PlatformLock
import io.github.mrcoder20.ioclens.core.concurrency.withLock

/**
 * Key-Value Storage interface for persistent settings in Common Kotlin.
 */
interface KeyValueStorage {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String): String
    fun setString(key: String, value: String)
    fun remove(key: String) {}
}

expect fun createPersistentKeyValueStorage(): KeyValueStorage

/**
 * Thread-safe in-memory KeyValueStorage for testing without Compose snapshot contention.
 */
class InMemoryKeyValueStorage : KeyValueStorage {
    private val lock = PlatformLock()
    private val booleanStorage = mutableMapOf<String, Boolean>()
    private val stringStorage = mutableMapOf<String, String>()

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = lock.withLock {
        booleanStorage[key] ?: defaultValue
    }

    override fun setBoolean(key: String, value: Boolean) {
        lock.withLock {
            booleanStorage[key] = value
        }
    }

    override fun getString(key: String, defaultValue: String): String = lock.withLock {
        stringStorage[key] ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        lock.withLock {
            stringStorage[key] = value
        }
    }

    override fun remove(key: String) {
        lock.withLock {
            booleanStorage.remove(key)
            stringStorage.remove(key)
        }
    }
}
