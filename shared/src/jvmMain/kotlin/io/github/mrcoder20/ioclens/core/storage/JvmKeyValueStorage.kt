package io.github.mrcoder20.ioclens.core.storage

import io.github.mrcoder20.ioclens.logDiagnostic
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.withLock

private const val ENC_PREFIX = "enc:v1:"
private const val GCM_IV_BYTES = 12
private const val GCM_TAG_BITS = 128
private const val MAX_CORRUPT_BACKUPS = 2
private const val HISTORY_KEY = "key_scan_history_json"

private val SENSITIVE_PREFIXES = listOf("key_vt_", "key_abuseipdb_", "key_alienvault_", "key_pulsedive_", "key_ipqualityscore_", "key_privacy_rules")

class JvmFileKeyValueStorage(
    private val storageFile: File
) : KeyValueStorage {

    private val lock = ReentrantLock()
    private val properties = Properties()
    private val random = SecureRandom()
    private val parentDir: File = storageFile.parentFile ?: File(".")
    private val historyFile: File = File(parentDir, "${storageFile.name}.history.enc")
    private val keyFile: File = File(parentDir, "${storageFile.name}.key")

    @Volatile
    private var cachedHistory: String = ""

    init {
        lock.withLock {
            loadPropertiesLocked()
            loadHistoryLocked()
        }
    }

    private fun isSensitiveKey(key: String): Boolean {
        return SENSITIVE_PREFIXES.any { key.startsWith(it) }
    }

    private fun getOrCreateMasterKeyLocked(): SecretKey {
        parentDir.mkdirs()
        if (keyFile.exists() && keyFile.isFile) {
            val encoded = runCatching { keyFile.readText(Charsets.UTF_8).trim() }.getOrNull()
            if (!encoded.isNullOrBlank()) {
                val decoded = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
                if (decoded != null && decoded.size == 32) {
                    return SecretKeySpec(decoded, "AES")
                }
            }
        }
        val keyBytes = ByteArray(32)
        random.nextBytes(keyBytes)
        val encoded = Base64.getEncoder().encodeToString(keyBytes)
        atomicWriteTextLocked(keyFile, encoded)
        restrictOwnerPermissions(keyFile)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun restrictOwnerPermissions(file: File) {
        val path = file.toPath()
        runCatching {
            val posix = PosixFilePermissions.fromString("rw-------")
            Files.setPosixFilePermissions(path, posix)
            return
        }
        runCatching {
            val aclView = Files.getFileAttributeView(path, AclFileAttributeView::class.java) ?: return
            val previousAcl = aclView.acl
            val currentUserName = System.getProperty("user.name")?.takeIf { it.isNotBlank() }
            val userPrincipal = if (currentUserName != null) {
                runCatching { path.fileSystem.userPrincipalLookupService.lookupPrincipalByName(currentUserName) }.getOrNull()
            } else {
                null
            }
            val disallowedNames = setOf("everyone", "builtin\\users", "users", "nt authority\\authenticated users", "authenticated users")
            val retained = previousAcl.filter { entry ->
                val pname = entry.principal().name.lowercase()
                pname !in disallowedNames
            }.toMutableList()

            val fullPerms = java.util.EnumSet.allOf(AclEntryPermission::class.java)
            listOfNotNull(userPrincipal, aclView.owner).distinct().forEach { principal ->
                if (retained.none { it.principal() == principal }) {
                    retained.add(
                        AclEntry.newBuilder()
                            .setType(AclEntryType.ALLOW)
                            .setPrincipal(principal)
                            .setPermissions(fullPerms)
                            .build()
                    )
                }
            }

            if (retained.isNotEmpty()) {
                aclView.acl = retained
                if (!Files.isReadable(path) || !Files.isWritable(path)) {
                    aclView.acl = previousAcl
                }
            }
        }
    }

    private fun encryptStringLocked(plaintext: String): String {
        val key = getOrCreateMasterKeyLocked()
        val iv = ByteArray(GCM_IV_BYTES)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        iv.copyInto(combined, 0)
        cipherBytes.copyInto(combined, iv.size)
        return ENC_PREFIX + Base64.getEncoder().encodeToString(combined)
    }

    private fun decryptStringLocked(stored: String): String {
        if (!stored.startsWith(ENC_PREFIX)) {
            return stored
        }
        val payload = Base64.getDecoder().decode(stored.removePrefix(ENC_PREFIX))
        require(payload.size > GCM_IV_BYTES) { "Corrupt encrypted payload" }
        val iv = payload.copyOfRange(0, GCM_IV_BYTES)
        val cipherBytes = payload.copyOfRange(GCM_IV_BYTES, payload.size)
        val key = getOrCreateMasterKeyLocked()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    private fun loadPropertiesLocked() {
        if (!storageFile.exists()) return
        try {
            storageFile.inputStream().use { properties.load(it) }
            // Migrate legacy inline history into dedicated encrypted history file
            val inlineHistory = properties.getProperty(HISTORY_KEY)
            if (!inlineHistory.isNullOrBlank()) {
                cachedHistory = runCatching { decryptStringLocked(inlineHistory) }.getOrDefault(inlineHistory)
                saveHistoryLocked(cachedHistory)
                properties.remove(HISTORY_KEY)
                savePropertiesLocked()
            }
        } catch (e: Exception) {
            logDiagnostic("JvmStorage", "Failed to load settings properties; quarantining corrupt file", e)
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
            logDiagnostic("JvmStorage", "Failed to decrypt scan history; quarantining corrupt file", e)
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
            corruptFiles.drop(MAX_CORRUPT_BACKUPS).forEach { it.delete() }
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
            restrictOwnerPermissions(storageFile)
        } catch (e: Exception) {
            tmpFile.delete()
            logDiagnostic("JvmStorage", "Failed to persist settings atomically", e)
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
            restrictOwnerPermissions(historyFile)
            cachedHistory = plaintext
        } catch (e: Exception) {
            logDiagnostic("JvmStorage", "Failed to persist encrypted scan history", e)
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
        if (key == HISTORY_KEY) {
            return cachedHistory.ifEmpty { defaultValue }
        }
        val raw = properties.getProperty(key) ?: return defaultValue
        if (!raw.startsWith(ENC_PREFIX)) {
            return raw
        }
        return try {
            decryptStringLocked(raw)
        } catch (e: Exception) {
            logDiagnostic("JvmStorage", "Decryption failed for key '$key'", e)
            defaultValue
        }
    }

    override fun setString(key: String, value: String) {
        lock.withLock {
            if (key == HISTORY_KEY) {
                saveHistoryLocked(value)
                return
            }
            val stored = if (isSensitiveKey(key) && value.isNotEmpty()) {
                try {
                    encryptStringLocked(value)
                } catch (e: Exception) {
                    logDiagnostic("JvmStorage", "Encryption failed for key '$key'", e)
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
            if (key == HISTORY_KEY) {
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
    val userHome = System.getProperty("user.home") ?: "."
    val dir = File(userHome, ".ioclens")
    dir.mkdirs()
    return JvmFileKeyValueStorage(File(dir, "settings.properties"))
}
