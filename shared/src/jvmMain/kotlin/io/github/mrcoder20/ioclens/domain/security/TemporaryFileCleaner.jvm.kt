@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

import io.github.mrcoder20.ioclens.logDiagnostic
import java.io.File

private val JVM_MANAGED_TEMP_PREFIXES = listOf(
    "picked_",
    "shared_stream_",
    "ioc_drop_",
    "ioc_pick_",
    "batch_",
    "share_"
)

actual object DefaultTemporaryFileCleaner : TemporaryFileCleaner {
    internal fun hasManagedPrefix(fileName: String): Boolean =
        JVM_MANAGED_TEMP_PREFIXES.any { fileName.startsWith(it) }

    actual override fun cleanIfTemporary(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        val file = File(filePath)
        if (!hasManagedPrefix(file.name)) {
            return false
        }
        return try {
            val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".").canonicalFile
            val canonicalFile = file.canonicalFile
            if (canonicalFile.toPath().startsWith(tempDir.toPath())) {
                if (canonicalFile.exists() && canonicalFile.isFile) {
                    canonicalFile.delete()
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            logDiagnostic("TempCleaner", "Failed to clean temporary file $filePath", e)
            false
        }
    }

    actual override fun sweepStaleTempFiles(maxAgeMs: Long): Int {
        return try {
            val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".").canonicalFile
            if (!tempDir.isDirectory) return 0
            val cutoff = System.currentTimeMillis() - maxAgeMs.coerceAtLeast(60_000L)
            var deletedCount = 0
            tempDir.listFiles()?.forEach { child ->
                if (child.isFile && hasManagedPrefix(child.name) && child.lastModified() in 1L until cutoff) {
                    if (child.delete()) deletedCount++
                }
            }
            deletedCount
        } catch (e: Exception) {
            logDiagnostic("TempCleaner", "Failed to sweep stale temp files", e)
            0
        }
    }
}
