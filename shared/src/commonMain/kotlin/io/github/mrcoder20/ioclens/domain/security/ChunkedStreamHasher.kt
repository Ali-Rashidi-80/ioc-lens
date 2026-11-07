package io.github.mrcoder20.ioclens.domain.security

data class FileHashResult(
    val sha256: String,
    val md5: String,
    val totalBytes: Long
)

expect class ChunkedStreamHasher() {
    suspend fun hashFile(filePath: String): FileHashResult
}
