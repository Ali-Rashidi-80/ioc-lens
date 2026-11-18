package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.NSLog
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getSystemLanguage(): AppLanguage {
    val lang = NSLocale.currentLocale.languageCode.lowercase()
    return when (lang) {
        "fa" -> AppLanguage.PERSIAN
        "ru" -> AppLanguage.RUSSIAN
        else -> AppLanguage.ENGLISH
    }
}

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun logDiagnostic(tag: String, message: String, throwable: Throwable?) {
    val redacted = message
        .replace(Regex("""([?&]key=)[^&\s]+""", RegexOption.IGNORE_CASE), "$1[REDACTED]")
        .replace(Regex("""(/api/json/ip/)[^/\s]+/""", RegexOption.IGNORE_CASE), "$1[REDACTED]/")
    val detail = throwable?.let { " | ${it::class.simpleName}: ${it.message.orEmpty()}" }.orEmpty()
    NSLog("IOCLens/%@: %@%@", tag, redacted, detail)
}
