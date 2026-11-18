@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

import io.github.mrcoder20.ioclens.core.storage.AndroidStorageContext
import io.github.mrcoder20.ioclens.logDiagnostic
import java.io.File

private val ANDROID_MANAGED_TEMP_PREFIXES = listOf(
    "picked_",
    "shared_stream_",
    "ioc_drop_",
    "ioc_pick_",
    "batch_",
    "share_"
)

actual object DefaultTemporaryFileCleaner : TemporaryFileCleaner {
    internal fun hasManagedPrefix(fileName: String): Boolean =
        ANDROID_MANAGED_TEMP_PREFIXES.any { fileName.startsWith(it) }

    private fun allowedTempRoots(): List<File> {
        val roots = mutableListOf<File>()
        AndroidStorageContext.applicationContext?.cacheDir?.let { roots.add(it.canonicalFile) }
        System.getProperty("java.io.tmpdir")?.takeIf { it.isNotBlank() }?.let {
            roots.add(File(it).canonicalFile)
        }
        return roots.distinct()
    }

    actual override fun cleanIfTemporary(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        val file = File(filePath)
        if (!hasManagedPrefix(file.name)) {
            return false
        }
        return try {
            val canonicalFile = file.canonicalFile
            val canonicalPathObj = canonicalFile.toPath()
            val insideTempRoot = allowedTempRoots().any { root ->
                canonicalPathObj.startsWith(root.toPath())
            }
            if (insideTempRoot && canonicalFile.exists() && canonicalFile.isFile) {
                canonicalFile.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            logDiagnostic("TempCleaner", "Failed to delete temp file", e)
            false
        }
    }

    actual override fun sweepStaleTempFiles(maxAgeMs: Long): Int {
        return try {
            val cutoff = System.currentTimeMillis() - maxAgeMs.coerceAtLeast(60_000L)
            var deletedCount = 0
            allowedTempRoots().forEach { root ->
                if (root.isDirectory) {
                    root.listFiles()?.forEach { child ->
                        if (child.isFile && hasManagedPrefix(child.name) && child.lastModified() in 1L until cutoff) {
                            if (child.delete()) deletedCount++
                        }
                    }
                }
            }
            deletedCount
        } catch (e: Exception) {
            logDiagnostic("TempCleaner", "Failed to sweep Android temp files", e)
            0
        }
    }
}
