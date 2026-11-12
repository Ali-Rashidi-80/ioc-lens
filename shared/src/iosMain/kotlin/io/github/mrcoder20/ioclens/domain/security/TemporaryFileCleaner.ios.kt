@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

import platform.Foundation.NSTemporaryDirectory
import platform.posix.unlink

actual object DefaultTemporaryFileCleaner : TemporaryFileCleaner {
    actual override fun cleanIfTemporary(filePath: String): Boolean {
        val fileName = filePath.substringAfterLast('/')
        if (!fileName.startsWith("picked_") && !fileName.startsWith("shared_stream_")) {
            return false
        }
        val tempDir = NSTemporaryDirectory()
        val isTmp = filePath.startsWith(tempDir) || filePath.contains("/tmp/") || filePath.contains("/Caches/")
        if (!isTmp) {
            return false
        }
        return unlink(filePath) == 0
    }
}
