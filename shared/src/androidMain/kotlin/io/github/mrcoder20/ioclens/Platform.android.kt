package io.github.mrcoder20.ioclens

import android.os.Build
import android.util.Log
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import java.util.Locale

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getSystemLanguage(): AppLanguage {
    val lang = Locale.getDefault().language.lowercase()
    return when (lang) {
        "fa" -> AppLanguage.PERSIAN
        "ru" -> AppLanguage.RUSSIAN
        else -> AppLanguage.ENGLISH
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun logDiagnostic(tag: String, message: String, throwable: Throwable?) {
    runCatching {
        if (throwable != null) {
            Log.w("IOCLens/$tag", message, throwable)
        } else {
            Log.i("IOCLens/$tag", message)
        }
    }
}
