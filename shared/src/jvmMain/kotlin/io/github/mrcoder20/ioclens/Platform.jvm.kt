package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getSystemLanguage(): AppLanguage {
    val lang = Locale.getDefault().language.lowercase()
    return when (lang) {
        "fa" -> AppLanguage.PERSIAN
        "ru" -> AppLanguage.RUSSIAN
        else -> AppLanguage.ENGLISH
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private object JvmRotatingLogger {
    private const val MAX_LOG_BYTES = 512 * 1024L
    private val lock = ReentrantLock()
    private val logDir: File by lazy {
        val home = System.getProperty("user.home") ?: "."
        File(home, ".ioclens/logs").apply { mkdirs() }
    }
    private val activeLog: File by lazy { File(logDir, "app.log") }
    private val backupLog: File by lazy { File(logDir, "app.log.1") }

    fun write(tag: String, message: String, throwable: Throwable?) {
        lock.withLock {
            runCatching {
                if (activeLog.exists() && activeLog.length() >= MAX_LOG_BYTES) {
                    if (backupLog.exists()) backupLog.delete()
                    activeLog.renameTo(backupLog)
                }
                val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())
                val sanitized = redactSecrets(message)
                val line = buildString {
                    append(ts).append(" [").append(tag).append("] ").append(sanitized)
                    if (throwable != null) {
                        append(" | ").append(throwable::class.simpleName)
                        val msg = throwable.message?.let { redactSecrets(it) }
                        if (!msg.isNullOrBlank()) append(": ").append(msg)
                    }
                    append('\n')
                }
                activeLog.appendText(line, Charsets.UTF_8)
            }
        }
    }

    private fun redactSecrets(input: String): String {
        return input
            .replace(Regex("""([?&]key=)[^&\s]+""", RegexOption.IGNORE_CASE), "$1[REDACTED]")
            .replace(Regex("""(/api/json/ip/)[^/\s]+/""", RegexOption.IGNORE_CASE), "$1[REDACTED]/")
    }
}

actual fun logDiagnostic(tag: String, message: String, throwable: Throwable?) {
    JvmRotatingLogger.write(tag, message, throwable)
}
