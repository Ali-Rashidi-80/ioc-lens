@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

import java.io.File

actual object DefaultTemporaryFileCleaner : TemporaryFileCleaner {
    actual override fun cleanIfTemporary(filePath: String): Boolean {
        val file = File(filePath)
        val name = file.name
        if (!name.startsWith("picked_") && !name.startsWith("shared_stream_")) {
            return false
        }
        return try {
            val canonicalPath = file.canonicalPath
            val tempDir = System.getProperty("java.io.tmpdir")
            val isCacheOrTmp = canonicalPath.contains("/cache/") ||
                canonicalPath.contains("/tmp/") ||
                canonicalPath.contains("cacheDir") ||
                (tempDir != null && canonicalPath.startsWith(File(tempDir).canonicalPath))
            if (isCacheOrTmp && file.exists() && file.isFile) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
