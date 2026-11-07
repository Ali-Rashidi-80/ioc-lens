package io.github.mrcoder20.ioclens.domain.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.UByteVar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreCrypto.CC_MD5_CTX
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import platform.CoreCrypto.CC_MD5_Final
import platform.CoreCrypto.CC_MD5_Init
import platform.CoreCrypto.CC_MD5_Update
import platform.CoreCrypto.CC_SHA256_CTX
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256_Final
import platform.CoreCrypto.CC_SHA256_Init
import platform.CoreCrypto.CC_SHA256_Update
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

actual class ChunkedStreamHasher actual constructor() {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun hashFile(filePath: String): FileHashResult = withContext(Dispatchers.Default) {
        val filePtr = fopen(filePath, "rb")
            ?: throw IllegalArgumentException("Cannot open file at: $filePath")

        try {
            memScoped {
                val sha256Ctx = alloc<CC_SHA256_CTX>()
                CC_SHA256_Init(sha256Ctx.ptr)

                val md5Ctx = alloc<CC_MD5_CTX>()
                CC_MD5_Init(md5Ctx.ptr)

                val bufferSize = 64 * 1024
                val buffer = allocArray<UByteVar>(bufferSize)
                var totalBytes = 0L

                while (true) {
                    val bytesRead = fread(buffer, 1.convert(), bufferSize.convert(), filePtr)
                    if (bytesRead > 0u) {
                        CC_SHA256_Update(sha256Ctx.ptr, buffer, bytesRead.convert())
                        CC_MD5_Update(md5Ctx.ptr, buffer, bytesRead.convert())
                        totalBytes += bytesRead.toLong()
                    } else {
                        break
                    }
                }

                val sha256Digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
                CC_SHA256_Final(sha256Digest, sha256Ctx.ptr)

                val md5Digest = allocArray<UByteVar>(CC_MD5_DIGEST_LENGTH)
                CC_MD5_Final(md5Digest, md5Ctx.ptr)

                val sha256Hex = (0 until CC_SHA256_DIGEST_LENGTH).joinToString("") { i ->
                    sha256Digest[i].toString(16).padStart(2, '0')
                }
                val md5Hex = (0 until CC_MD5_DIGEST_LENGTH).joinToString("") { i ->
                    md5Digest[i].toString(16).padStart(2, '0')
                }

                FileHashResult(
                    sha256 = sha256Hex,
                    md5 = md5Hex,
                    totalBytes = totalBytes
                )
            }
        } finally {
            fclose(filePtr)
        }
    }
}


