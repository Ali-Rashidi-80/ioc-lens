@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

actual class ChunkedStreamHasher actual constructor() {

    actual suspend fun hashFile(filePath: String): FileHashResult = withContext(Dispatchers.Default) {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("File not found or is not a regular file at: $filePath")
        }

        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val md5Digest = MessageDigest.getInstance("MD5")

        val buffer = ByteArray(64 * 1024) // 64KB buffer
        var totalBytes = 0L

        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
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
