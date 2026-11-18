package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getSystemLanguage(): AppLanguage

expect fun currentTimeMillis(): Long

expect fun logDiagnostic(tag: String, message: String, throwable: Throwable? = null)
