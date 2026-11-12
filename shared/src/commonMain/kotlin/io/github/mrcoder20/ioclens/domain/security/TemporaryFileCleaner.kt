@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.github.mrcoder20.ioclens.domain.security

interface TemporaryFileCleaner {
    fun cleanIfTemporary(filePath: String): Boolean
}

expect object DefaultTemporaryFileCleaner : TemporaryFileCleaner {
    override fun cleanIfTemporary(filePath: String): Boolean
}
