@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

interface TemporaryFileCleaner {
    fun cleanIfTemporary(filePath: String): Boolean
    fun sweepStaleTempFiles(maxAgeMs: Long = 3_600_000L): Int = 0
}

expect object DefaultTemporaryFileCleaner : TemporaryFileCleaner {
    override fun cleanIfTemporary(filePath: String): Boolean
    override fun sweepStaleTempFiles(maxAgeMs: Long): Int
}
