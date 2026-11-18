package io.github.mrcoder20.ioclens.core.storage

import io.github.mrcoder20.ioclens.core.concurrency.PlatformLock
import io.github.mrcoder20.ioclens.core.concurrency.withLock
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionComplete
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToURL

interface IosSecureKeychainDelegate {
    fun getSecret(key: String): String?
    fun setSecret(key: String, value: String)
    fun removeSecret(key: String)
}

object IosPlatformStorageBridge {
    @Volatile
    var keychainDelegate: IosSecureKeychainDelegate? = null
}

private const val IOS_HISTORY_KEY = "key_scan_history_json"
private val IOS_SENSITIVE_PREFIXES = listOf(
    "key_vt_",
    "key_abuseipdb_",
    "key_alienvault_",
    "key_pulsedive_",
    "key_ipqualityscore_",
    "key_privacy_rules"
)

class IosKeyValueStorage : KeyValueStorage {
    private val lock = PlatformLock()
    private val defaults = NSUserDefaults.standardUserDefaults
    private val fileManager = NSFileManager.defaultManager

    private fun isProtectedKey(key: String): Boolean {
        return key == IOS_HISTORY_KEY || IOS_SENSITIVE_PREFIXES.any { key.startsWith(it) }
    }

    private fun protectedFileUrl(key: String): NSURL? {
        val urls = fileManager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)
        val appSupport = (urls.firstOrNull() as? NSURL) ?: return null
        val dirUrl = appSupport.URLByAppendingPathComponent("ioclens", isDirectory = true) ?: return null
        dirUrl.path?.let { dirPath ->
            if (!fileManager.fileExistsAtPath(dirPath)) {
                fileManager.createDirectoryAtURL(dirUrl, withIntermediateDirectories = true, attributes = null, error = null)
            }
        }
        val safeName = key.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".dat"
        return dirUrl.URLByAppendingPathComponent(safeName)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = lock.withLock {
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else defaultValue
    }

    override fun setBoolean(key: String, value: Boolean) {
        lock.withLock {
            defaults.setBool(value, forKey = key)
        }
    }

    override fun getString(key: String, defaultValue: String): String = lock.withLock {
        if (isProtectedKey(key)) {
            val delegate = IosPlatformStorageBridge.keychainDelegate
            if (delegate != null) {
                return delegate.getSecret(key) ?: defaultValue
            }
            val fileUrl = protectedFileUrl(key)
            val filePath = fileUrl?.path
            if (filePath != null && fileManager.fileExistsAtPath(filePath)) {
                val data = NSData.create(contentsOfFile = filePath)
                if (data != null) {
                    val str = NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
                    if (str != null) return str
                }
            }
            // Migrate any legacy value out of NSUserDefaults
            val legacy = defaults.stringForKey(key)
            if (legacy != null) {
                setProtectedValueLocked(key, legacy)
                defaults.removeObjectForKey(key)
                return legacy
            }
            return defaultValue
        }
        return defaults.stringForKey(key) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        lock.withLock {
            if (isProtectedKey(key)) {
                setProtectedValueLocked(key, value)
                defaults.removeObjectForKey(key)
            } else {
                defaults.setObject(value, forKey = key)
            }
        }
    }

    override fun remove(key: String) {
        lock.withLock {
            if (isProtectedKey(key)) {
                IosPlatformStorageBridge.keychainDelegate?.removeSecret(key)
                protectedFileUrl(key)?.path?.let { filePath ->
                    if (fileManager.fileExistsAtPath(filePath)) {
                        fileManager.removeItemAtPath(filePath, error = null)
                    }
                }
            }
            defaults.removeObjectForKey(key)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun setProtectedValueLocked(key: String, value: String) {
        val delegate = IosPlatformStorageBridge.keychainDelegate
        if (delegate != null) {
            if (value.isEmpty()) delegate.removeSecret(key) else delegate.setSecret(key, value)
            return
        }
        val fileUrl = protectedFileUrl(key) ?: return
        val filePath = fileUrl.path ?: return
        if (value.isEmpty()) {
            if (fileManager.fileExistsAtPath(filePath)) {
                fileManager.removeItemAtPath(filePath, error = null)
            }
            return
        }
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        data.writeToURL(fileUrl, atomically = true)
        fileManager.setAttributes(
            mapOf(NSFileProtectionKey to NSFileProtectionComplete),
            ofItemAtPath = filePath,
            error = null
        )
    }
}

actual fun createPersistentKeyValueStorage(): KeyValueStorage = IosKeyValueStorage()
