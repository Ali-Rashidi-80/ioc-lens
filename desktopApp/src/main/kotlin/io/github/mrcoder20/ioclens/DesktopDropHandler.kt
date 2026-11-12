package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocaleManager
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import java.io.File

object DesktopDropHandler {
    fun processDroppedFiles(
        files: List<*>?,
        language: AppLanguage = LocaleManager.currentLanguage.value,
        onPayloadReady: (SharedPayload) -> Unit
    ): Boolean {
        val firstFile = files?.firstOrNull() as? File ?: return false
        if (!firstFile.exists()) return false

        return when {
            firstFile.isFile -> {
                onPayloadReady(
                    SharedPayload(
                        filePath = firstFile.absolutePath,
                        fileName = firstFile.name,
                        isTemporary = false
                    )
                )
                true
            }
            firstFile.isDirectory -> {
                val errorMsg = when (language) {
                    AppLanguage.PERSIAN -> "پوشه «${firstFile.name}» قابل تحلیل نیست؛ لطفاً یک فایل منفرد را برای محاسبه هش انتخاب کنید."
                    AppLanguage.RUSSIAN -> "Каталог «${firstFile.name}» не поддерживается. Перетащите отдельный файл для хеширования."
                    AppLanguage.ENGLISH -> "Cannot analyze folder '${firstFile.name}'. Please drop a single file to calculate its hash."
                }
                onPayloadReady(SharedPayload(errorMessage = errorMsg))
                true
            }
            else -> false
        }
    }

    fun processStartupArg(
        arg: String,
        language: AppLanguage = LocaleManager.currentLanguage.value,
        onPayloadReady: (SharedPayload) -> Unit
    ) {
        val file = File(arg)
        if (file.exists()) {
            if (file.isFile) {
                onPayloadReady(
                    SharedPayload(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        isTemporary = false
                    )
                )
            } else if (file.isDirectory) {
                val errorMsg = when (language) {
                    AppLanguage.PERSIAN -> "پوشه «${file.name}» قابل تحلیل نیست؛ لطفاً یک فایل منفرد را برای محاسبه هش انتخاب کنید."
                    AppLanguage.RUSSIAN -> "Каталог «${file.name}» не поддерживается. Перетащите отдельный файл для хеширования."
                    AppLanguage.ENGLISH -> "Cannot analyze folder '${file.name}'. Please drop a single file to calculate its hash."
                }
                onPayloadReady(SharedPayload(errorMessage = errorMsg))
            }
        } else {
            onPayloadReady(SharedPayload(textOrUrl = arg))
        }
    }
}
