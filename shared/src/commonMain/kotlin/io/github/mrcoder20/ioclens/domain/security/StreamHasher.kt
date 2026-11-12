package io.github.mrcoder20.ioclens.domain.security

interface StreamHasher {
    suspend fun hashFile(filePath: String): FileHashResult
}
