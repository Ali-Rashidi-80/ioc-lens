package io.github.mrcoder20.ioclens.domain.security

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemporaryFileCleanerTest {

    @Test
    fun testPickedFileInsideTempDirIsCleaned() {
        val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".").canonicalFile
        val tempFile = File(tempDir, "picked_${System.currentTimeMillis()}_sample.bin")
        tempFile.writeText("test payload")
        assertTrue(tempFile.exists())

        val cleaned = DefaultTemporaryFileCleaner.cleanIfTemporary(tempFile.absolutePath)
        assertTrue(cleaned, "Picked file in tmpdir must be successfully cleaned")
        assertFalse(tempFile.exists(), "File should no longer exist on disk")
    }

    @Test
    fun testSharedStreamFileInsideTempDirIsCleaned() {
        val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".").canonicalFile
        val tempFile = File(tempDir, "shared_stream_${System.currentTimeMillis()}_file.txt")
        tempFile.writeText("shared content")
        assertTrue(tempFile.exists())

        val cleaned = DefaultTemporaryFileCleaner.cleanIfTemporary(tempFile.absolutePath)
        assertTrue(cleaned, "Shared stream file in tmpdir must be successfully cleaned")
        assertFalse(tempFile.exists(), "File should no longer exist on disk")
    }

    @Test
    fun testArbitraryUserFileInsideTempDirIsNotCleaned() {
        val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".").canonicalFile
        val userFile = File(tempDir, "user_document_${System.currentTimeMillis()}.pdf")
        userFile.writeText("important document")
        try {
            assertTrue(userFile.exists())

            val cleaned = DefaultTemporaryFileCleaner.cleanIfTemporary(userFile.absolutePath)
            assertFalse(cleaned, "File without picked_/shared_stream_ prefix must NEVER be cleaned")
            assertTrue(userFile.exists(), "File must be preserved on disk")
        } finally {
            userFile.delete()
        }
    }

    @Test
    fun testPickedFileOutsideTempDirIsNotCleaned() {
        // Create file in current working directory (user workspace), outside java.io.tmpdir
        val currentDir = File(".").canonicalFile
        val outsideFile = File(currentDir, "picked_${System.currentTimeMillis()}_desktop.bin")
        outsideFile.writeText("desktop file")
        try {
            assertTrue(outsideFile.exists())

            val cleaned = DefaultTemporaryFileCleaner.cleanIfTemporary(outsideFile.absolutePath)
            assertFalse(cleaned, "File outside temporary directory must NEVER be cleaned even with picked_ prefix")
            assertTrue(outsideFile.exists(), "User file must be preserved on disk")
        } finally {
            outsideFile.delete()
        }
    }

    @Test
    fun testNonExistentFileReturnsFalse() {
        val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".").canonicalFile
        val fakeFile = File(tempDir, "picked_999999_nonexistent.bin")
        val cleaned = DefaultTemporaryFileCleaner.cleanIfTemporary(fakeFile.absolutePath)
        assertFalse(cleaned)
    }
}
