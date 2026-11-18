@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

actual class ChunkedStreamHasher actual constructor() : StreamHasher {

    actual override suspend fun hashFile(filePath: String): FileHashResult = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || !file.isFile || !file.canRead()) {
            throw IllegalArgumentException("Selected file does not exist or cannot be read")
        }

        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val md5Digest = MessageDigest.getInstance("MD5")

        val buffer = ByteArray(64 * 1024)
        var totalBytes = 0L

        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                ensureActive()
                sha256Digest.update(buffer, 0, bytesRead)
                md5Digest.update(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
        }

        FileHashResult(
            sha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) },
            md5 = md5Digest.digest().joinToString("") { "%02x".format(it) },
            totalBytes = totalBytes
        )
    }
}
