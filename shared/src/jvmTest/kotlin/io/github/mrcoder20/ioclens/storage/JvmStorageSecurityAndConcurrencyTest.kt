package io.github.mrcoder20.ioclens.storage

import io.github.mrcoder20.ioclens.core.storage.JvmFileKeyValueStorage
import io.github.mrcoder20.ioclens.domain.security.DefaultTemporaryFileCleaner
import io.github.mrcoder20.ioclens.domain.vision.PlatformVisualDecoder
import io.github.mrcoder20.ioclens.presentation.vision.DesktopDragAndDropCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmStorageSecurityAndConcurrencyTest {

    @Test
    fun testEncryptedSecretStorageRoundTripAndDiskCiphertext() {
        val dir = Files.createTempDirectory("ioclens_sec_store").toFile()
        try {
            val storeFile = File(dir, "settings.properties")
            val secret = "vt-live-test-key-0123456789abcdef"
            val historyJson = """[{"indicator":{"rawValue":"8.8.8.8","normalizedValue":"8.8.8.8","type":"IP_ADDRESS"},"overallVerdict":"CLEAN","confidenceScore":95,"summaryDescription":"Clean","sourceResults":[]}]"""

            val storage1 = JvmFileKeyValueStorage(storeFile)
            storage1.setString("key_vt_api", secret)
            storage1.setString("key_scan_history_json", historyJson)

            val rawPropsOnDisk = storeFile.readText(Charsets.UTF_8)
            assertFalse(rawPropsOnDisk.contains(secret), "Plaintext API key must never exist on disk")
            assertTrue(rawPropsOnDisk.contains("enc\\:v1\\:") || rawPropsOnDisk.contains("enc:v1:"), "Encrypted prefix expected")

            val historyEncFile = File(dir, "settings.properties.history.enc")
            assertTrue(historyEncFile.exists(), "Encrypted history file must exist")
            val rawHistoryOnDisk = historyEncFile.readText(Charsets.UTF_8)
            assertFalse(rawHistoryOnDisk.contains("8.8.8.8"), "Plaintext IOC history must not appear on disk")
            assertTrue(rawHistoryOnDisk.startsWith("enc:v1:"))

            val storage2 = JvmFileKeyValueStorage(storeFile)
            assertEquals(secret, storage2.getString("key_vt_api", ""))
            assertEquals(historyJson, storage2.getString("key_scan_history_json", ""))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testConcurrentReadsAndWritesDoNotCorruptStorage() = runBlocking {
        val dir = Files.createTempDirectory("ioclens_conc_store").toFile()
        try {
            val storeFile = File(dir, "concurrent.properties")
            val storage = JvmFileKeyValueStorage(storeFile)

            val jobs = (0 until 8).map { workerIdx ->
                async(Dispatchers.IO) {
                    repeat(20) { iter ->
                        val key = "key_vt_worker_$workerIdx"
                        val valStr = "secret_${workerIdx}_$iter"
                        storage.setString(key, valStr)
                        storage.setBoolean("flag_$workerIdx", iter % 2 == 0)
                        storage.getString(key, "")
                    }
                }
            }
            jobs.awaitAll()

            val reloaded = JvmFileKeyValueStorage(storeFile)
            for (workerIdx in 0 until 8) {
                assertEquals("secret_${workerIdx}_19", reloaded.getString("key_vt_worker_$workerIdx", ""))
                assertFalse(reloaded.getBoolean("flag_$workerIdx", true))
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testCorruptEncryptedHistoryQuarantinesWithoutCrashing() {
        val dir = Files.createTempDirectory("ioclens_corrupt_store").toFile()
        try {
            val storeFile = File(dir, "settings.properties")
            val historyEncFile = File(dir, "settings.properties.history.enc")
            historyEncFile.writeText("enc:v1:not_valid_base64_@@@_corrupt", Charsets.UTF_8)

            val storage = JvmFileKeyValueStorage(storeFile)
            assertEquals("fallback", storage.getString("key_scan_history_json", "fallback"))

            val quarantined = dir.listFiles { f -> f.name.contains(".corrupt") }.orEmpty()
            assertTrue(quarantined.isNotEmpty(), "Corrupt encrypted history must be quarantined before rewrite")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun testTemporaryFileCleanerEnforcesPrefixAndRootContainment() {
        val repoRelativeNonTmp = File(System.getProperty("user.dir"), "build/tmp_cleaner_guard_test.tmp")
        repoRelativeNonTmp.parentFile.mkdirs()
        repoRelativeNonTmp.writeText("keep me", Charsets.UTF_8)

        val validShare = File.createTempFile("share_", ".tmp")
        val validBatch = File.createTempFile("batch_", ".tmp")
        val unprefixedTmp = File.createTempFile("user_doc_", ".tmp")
        try {
            val sneakyOutside = File(repoRelativeNonTmp.parentFile, "share_sneaky.tmp").apply {
                writeText("protected", Charsets.UTF_8)
            }
            assertFalse(DefaultTemporaryFileCleaner.cleanIfTemporary(sneakyOutside.absolutePath))
            assertTrue(sneakyOutside.exists(), "File outside temp root must never be deleted")

            assertFalse(DefaultTemporaryFileCleaner.cleanIfTemporary(unprefixedTmp.absolutePath))
            assertTrue(unprefixedTmp.exists(), "Unmanaged prefix file in temp dir must never be deleted")

            assertTrue(DefaultTemporaryFileCleaner.cleanIfTemporary(validShare.absolutePath))
            assertFalse(validShare.exists())

            assertTrue(DefaultTemporaryFileCleaner.cleanIfTemporary(validBatch.absolutePath))
            assertFalse(validBatch.exists())

            assertFalse(DefaultTemporaryFileCleaner.cleanIfTemporary(repoRelativeNonTmp.absolutePath))
        } finally {
            repoRelativeNonTmp.delete()
            File(repoRelativeNonTmp.parentFile, "share_sneaky.tmp").delete()
            validShare.delete()
            validBatch.delete()
            unprefixedTmp.delete()
        }
    }

    @Test
    fun testVisualDecoderRejectsOversizedImageBytesAndDndRejectsUncPaths() {
        val oversized = ByteArray(20 * 1024 * 1024 + 1)
        assertTrue(PlatformVisualDecoder().decodeBarcodeFromBytes(oversized).isEmpty())

        assertTrue(DesktopDragAndDropCoordinator.isUnsafeRemotePath("\\\\evil.example\\share\\qr.png"))
        assertTrue(DesktopDragAndDropCoordinator.isUnsafeRemotePath("file://evil.example/share/qr.png"))
        assertNull(DesktopDragAndDropCoordinator.parseDroppedTextToFile("\\\\evil.example\\share\\qr.png"))
    }
}
