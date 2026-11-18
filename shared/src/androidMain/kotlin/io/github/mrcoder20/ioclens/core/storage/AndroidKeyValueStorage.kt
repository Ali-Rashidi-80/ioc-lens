package io.github.mrcoder20.ioclens.core.storage

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.mrcoder20.ioclens.logDiagnostic
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.withLock

private const val ANDROID_ENC_PREFIX = "enc:v1:"
private const val ANDROID_KEYSTORE_ALIAS = "ioclens_master_key_v1"
private const val ANDROID_GCM_IV_BYTES = 12
private const val ANDROID_GCM_TAG_BITS = 128
private const val ANDROID_MAX_CORRUPT_BACKUPS = 2
private const val ANDROID_HISTORY_KEY = "key_scan_history_json"

private val ANDROID_SENSITIVE_PREFIXES = listOf(
    "key_vt_",
    "key_abuseipdb_",
    "key_alienvault_",
    "key_pulsedive_",
    "key_ipqualityscore_",
    "key_privacy_rules"
)

object AndroidStorageContext {
    @Volatile
    var applicationContext: Context? = null
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}

class PersistentFileKeyValueStorage(
    private val storageFile: File
) : KeyValueStorage {

    private val lock = ReentrantLock()
    private val properties = Properties()
    private val random = SecureRandom()
    private val parentDir: File = storageFile.parentFile ?: File(".")
    private val historyFile: File = File(parentDir, "${storageFile.name}.history.enc")
    private val fallbackKeyFile: File = File(parentDir, "${storageFile.name}.key")

    @Volatile
    private var cachedHistory: String = ""

    init {
        lock.withLock {
            loadPropertiesLocked()
            loadHistoryLocked()
        }
    }

    private fun isSensitiveKey(key: String): Boolean {
        return ANDROID_SENSITIVE_PREFIXES.any { key.startsWith(it) }
    }

    private fun getOrCreateSecretKeyLocked(): SecretKey {
        // Prefer hardware-backed AndroidKeyStore on real Android devices
        val keystoreResult = runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            val existing = ks.getKey(ANDROID_KEYSTORE_ALIAS, null) as? SecretKey
            if (existing != null) return@runCatching existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                ANDROID_KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
        keystoreResult.getOrNull()?.let { return it }

        // Host-test / Robolectric fallback inside app-private filesDir
        parentDir.mkdirs()
        if (fallbackKeyFile.exists() && fallbackKeyFile.isFile) {
            val encoded = runCatching { fallbackKeyFile.readText(Charsets.UTF_8).trim() }.getOrNull()
            if (!encoded.isNullOrBlank()) {
                val decoded = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
                if (decoded != null && decoded.size == 32) {
                    return SecretKeySpec(decoded, "AES")
                }
            }
        }
        val keyBytes = ByteArray(32)
        random.nextBytes(keyBytes)
        atomicWriteTextLocked(fallbackKeyFile, Base64.getEncoder().encodeToString(keyBytes))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encryptStringLocked(plaintext: String): String {
        val key = getOrCreateSecretKeyLocked()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + cipherBytes.size)
        combined[0] = iv.size.toByte()
        iv.copyInto(combined, 1)
        cipherBytes.copyInto(combined, 1 + iv.size)
        return ANDROID_ENC_PREFIX + Base64.getEncoder().encodeToString(combined)
    }

    private fun decryptStringLocked(stored: String): String {
        if (!stored.startsWith(ANDROID_ENC_PREFIX)) {
            return stored
        }
        val payload = Base64.getDecoder().decode(stored.removePrefix(ANDROID_ENC_PREFIX))
        require(payload.size > 2) { "Corrupt encrypted payload" }
        val ivLen = payload[0].toInt() and 0xFF
        val (iv, cipherBytes) = if (ivLen in 12..16 && payload.size > 1 + ivLen) {
            payload.copyOfRange(1, 1 + ivLen) to payload.copyOfRange(1 + ivLen, payload.size)
        } else {
            require(payload.size > ANDROID_GCM_IV_BYTES)
            payload.copyOfRange(0, ANDROID_GCM_IV_BYTES) to payload.copyOfRange(ANDROID_GCM_IV_BYTES, payload.size)
        }
        val key = getOrCreateSecretKeyLocked()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(ANDROID_GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    private fun loadPropertiesLocked() {
        if (!storageFile.exists()) return
        try {
            storageFile.inputStream().use { properties.load(it) }
            val inlineHistory = properties.getProperty(ANDROID_HISTORY_KEY)
            if (!inlineHistory.isNullOrBlank()) {
                cachedHistory = runCatching { decryptStringLocked(inlineHistory) }.getOrDefault(inlineHistory)
                saveHistoryLocked(cachedHistory)
                properties.remove(ANDROID_HISTORY_KEY)
                savePropertiesLocked()
            }
        } catch (e: Exception) {
            logDiagnostic("AndroidStorage", "Corrupt settings properties quarantined", e)
            quarantineCorruptFileLocked(storageFile)
            properties.clear()
        }
    }

    private fun loadHistoryLocked() {
        if (!historyFile.exists()) return
        try {
            val raw = historyFile.readText(Charsets.UTF_8).trim()
            cachedHistory = if (raw.isBlank()) "" else decryptStringLocked(raw)
        } catch (e: Exception) {
            logDiagnostic("AndroidStorage", "Corrupt encrypted history quarantined", e)
            quarantineCorruptFileLocked(historyFile)
            cachedHistory = ""
        }
    }

    private fun quarantineCorruptFileLocked(target: File) {
        runCatching {
            val backup = File(parentDir, "${target.name}.corrupt.${System.currentTimeMillis()}")
            target.renameTo(backup)
            val corruptFiles = parentDir.listFiles { _, name ->
                name.startsWith("${target.name}.corrupt.")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            corruptFiles.drop(ANDROID_MAX_CORRUPT_BACKUPS).forEach { it.delete() }
        }
    }

    private fun atomicWriteTextLocked(target: File, text: String) {
        parentDir.mkdirs()
        val tmpFile = File(parentDir, "${target.name}.tmp")
        FileOutputStream(tmpFile).use { fos ->
            fos.write(text.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.fd.sync()
        }
        try {
            Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun savePropertiesLocked() {
        parentDir.mkdirs()
        val tmpFile = File(parentDir, "${storageFile.name}.tmp")
        try {
            FileOutputStream(tmpFile).use { fos ->
                properties.store(fos, "IOC Lens Persistent Settings")
                fos.flush()
                fos.fd.sync()
            }
            try {
                Files.move(tmpFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(tmpFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            tmpFile.delete()
            logDiagnostic("AndroidStorage", "Failed to save settings properties", e)
        }
    }

    private fun saveHistoryLocked(plaintext: String) {
        try {
            if (plaintext.isBlank()) {
                if (historyFile.exists()) historyFile.delete()
                cachedHistory = ""
                return
            }
            val encrypted = encryptStringLocked(plaintext)
            atomicWriteTextLocked(historyFile, encrypted)
            cachedHistory = plaintext
        } catch (e: Exception) {
            logDiagnostic("AndroidStorage", "Failed to persist encrypted scan history", e)
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = lock.withLock {
        val valStr = properties.getProperty(key) ?: return defaultValue
        return valStr.toBooleanStrictOrNull() ?: defaultValue
    }

    override fun setBoolean(key: String, value: Boolean) {
        lock.withLock {
            properties.setProperty(key, value.toString())
            savePropertiesLocked()
        }
    }

    override fun getString(key: String, defaultValue: String): String = lock.withLock {
        if (key == ANDROID_HISTORY_KEY) {
            return cachedHistory.ifEmpty { defaultValue }
        }
        val raw = properties.getProperty(key) ?: return defaultValue
        if (!raw.startsWith(ANDROID_ENC_PREFIX)) {
            return raw
        }
        return try {
            decryptStringLocked(raw)
        } catch (e: Exception) {
            logDiagnostic("AndroidStorage", "Decryption failed for key '$key'", e)
            defaultValue
        }
    }

    override fun setString(key: String, value: String) {
        lock.withLock {
            if (key == ANDROID_HISTORY_KEY) {
                saveHistoryLocked(value)
                return
            }
            val stored = if (isSensitiveKey(key) && value.isNotEmpty()) {
                try {
                    encryptStringLocked(value)
                } catch (e: Exception) {
                    logDiagnostic("AndroidStorage", "Encryption failed for key '$key'", e)
                    return
                }
            } else {
                value
            }
            properties.setProperty(key, stored)
            savePropertiesLocked()
        }
    }

    override fun remove(key: String) {
        lock.withLock {
            if (key == ANDROID_HISTORY_KEY) {
                if (historyFile.exists()) historyFile.delete()
                cachedHistory = ""
                return
            }
            if (properties.remove(key) != null) {
                savePropertiesLocked()
            }
        }
    }
}

actual fun createPersistentKeyValueStorage(): KeyValueStorage {
    val context = AndroidStorageContext.applicationContext ?: runCatching {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentAppMethod = activityThreadClass.getMethod("currentApplication")
        currentAppMethod.invoke(null) as? Context
    }.getOrNull()

    val storageFile = if (context != null) {
        val dir = File(context.filesDir, "ioclens")
        dir.mkdirs()
        File(dir, "settings.properties")
    } else {
        // Host unit test fallback only; real Android devices always initialize AndroidStorageContext in MainActivity
        val isRealDevice = Build.FINGERPRINT != null && !Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
        if (isRealDevice) {
            error("AndroidStorageContext must be initialized before creating persistent storage")
        }
        val dir = File(System.getProperty("java.io.tmpdir") ?: ".", "ioclens-test")
        dir.mkdirs()
        File(dir, "settings.properties")
    }
    return PersistentFileKeyValueStorage(storageFile)
}
