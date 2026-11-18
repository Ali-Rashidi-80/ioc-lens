@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.posix.unlink

private val IOS_MANAGED_TEMP_PREFIXES = listOf(
    "picked_",
    "shared_stream_",
    "ioc_drop_",
    "ioc_pick_",
    "batch_",
    "share_"
)

actual object DefaultTemporaryFileCleaner : TemporaryFileCleaner {
    private fun hasManagedPrefix(fileName: String): Boolean =
        IOS_MANAGED_TEMP_PREFIXES.any { fileName.startsWith(it) }

    actual override fun cleanIfTemporary(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        val normalized = filePath.replace('\\', '/')
        if (normalized.contains("/../") || normalized.endsWith("/..")) return false
        val fileName = normalized.substringAfterLast('/')
        if (!hasManagedPrefix(fileName)) return false
        val tempRoot = NSTemporaryDirectory().trimEnd('/')
        val isTmp = normalized.startsWith("$tempRoot/") || normalized.startsWith("/tmp/")
        if (!isTmp) return false
        return unlink(normalized) == 0
    }

    actual override fun sweepStaleTempFiles(maxAgeMs: Long): Int {
        val fileManager = NSFileManager.defaultManager
        val tempRoot = NSTemporaryDirectory().trimEnd('/')
        val contents = fileManager.contentsOfDirectoryAtPath(tempRoot, error = null) ?: return 0
        val cutoffEpochSec = NSDate.date().timeIntervalSince1970 - (maxAgeMs.coerceAtLeast(60_000L) / 1000.0)
        var deletedCount = 0
        for (entry in contents) {
            val name = entry?.toString() ?: continue
            if (!hasManagedPrefix(name)) continue
            val fullPath = "$tempRoot/$name"
            val attrs = fileManager.attributesOfItemAtPath(fullPath, error = null)
            val modDate = attrs?.get(NSFileModificationDate) as? NSDate ?: continue
            if (modDate.timeIntervalSince1970 in 1.0..cutoffEpochSec) {
                if (fileManager.removeItemAtPath(fullPath, error = null)) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }
}
