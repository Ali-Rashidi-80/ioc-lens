package io.github.mrcoder20.ioclens.domain.security

import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkedStreamHasherTest {

    private lateinit var tempFile: File
    private val expectedSize = 100L * 1024 * 1024 // 100MB

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("ioc_lens_test_100mb", ".bin")
        RandomAccessFile(tempFile, "rw").use { raf ->
            raf.setLength(expectedSize)
            raf.seek(0)
            raf.write("IOC_LENS_HEADER_SECURE_PAYLOAD".toByteArray())
            raf.seek(expectedSize - 32)
            raf.write("IOC_LENS_FOOTER_SECURE_PAYLOAD".toByteArray())
        }
    }

    @AfterTest
    fun tearDown() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testSynthetic100MbFileStreamingHash() = runBlocking {
        val hasher = ChunkedStreamHasher()
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        val result = hasher.hashFile(tempFile.absolutePath)

        runtime.gc()
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryDeltaMb = (memoryAfter - memoryBefore) / (1024 * 1024)

        // Memory delta should remain bounded (< 50MB) and not load 100MB file into heap
        assertTrue(memoryDeltaMb < 50, "Memory consumption ($memoryDeltaMb MB) exceeded bounds!")
        assertEquals(expectedSize, result.totalBytes)

        val expectedSha256 = computeIndependentDigest(tempFile, "SHA-256")
        val expectedMd5 = computeIndependentDigest(tempFile, "MD5")

        assertEquals(expectedSha256, result.sha256)
        assertEquals(expectedMd5, result.md5)
    }

    @Test
    fun testEmpty0ByteFileStreamingHash() = runBlocking {
        val emptyFile = File.createTempFile("ioc_lens_empty", ".bin")
        try {
            val hasher = ChunkedStreamHasher()
            val result = hasher.hashFile(emptyFile.absolutePath)
            assertEquals(0L, result.totalBytes)
            // Empty string standard hashes
            assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result.sha256)
            assertEquals("d41d8cd98f00b204e9800998ecf8427e", result.md5)
        } finally {
            emptyFile.delete()
        }
    }

    @Test
    fun testNonExistentFileThrowsException() = runBlocking {
        val hasher = ChunkedStreamHasher()
        var caught = false
        try {
            hasher.hashFile("non_existent_file_path_123456.bin")
        } catch (_: IllegalArgumentException) {
            caught = true
        }
        assertTrue(caught, "Expected IllegalArgumentException for non-existent file")
    }

    private fun computeIndependentDigest(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
