package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.components.PickedFile
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
        val allFiles = files?.filterIsInstance<File>() ?: return false
        if (allFiles.isEmpty()) return false

        val validFiles = allFiles.filter { it.isFile && it.exists() }
        val dirs = allFiles.filter { it.isDirectory && it.exists() }

        if (dirs.isNotEmpty() && validFiles.isEmpty()) {
            val firstDir = dirs.first()
            val errorMsg = when (language) {
                AppLanguage.PERSIAN -> "پوشه «${firstDir.name}» قابل تحلیل نیست؛ لطفاً یک یا چند فایل را برای محاسبه هش انتخاب کنید."
                AppLanguage.RUSSIAN -> "Каталог «${firstDir.name}» не поддерживается. Перетащите файлы для хеширования."
                AppLanguage.ENGLISH -> "Cannot analyze folder '${firstDir.name}'. Please drop files to calculate their hash."
            }
            onPayloadReady(SharedPayload(errorMessage = errorMsg))
            return true
        }

        if (validFiles.size > 1) {
            val batch = validFiles.map { PickedFile(it.absolutePath, it.name, it.length()) }
            onPayloadReady(
                SharedPayload(
                    filePath = validFiles.first().absolutePath,
                    fileName = validFiles.first().name,
                    batchFiles = batch,
                    isTemporary = false
                )
            )
            return true
        } else if (validFiles.size == 1) {
            val single = validFiles.first()
            onPayloadReady(
                SharedPayload(
                    filePath = single.absolutePath,
                    fileName = single.name,
                    batchFiles = listOf(PickedFile(single.absolutePath, single.name, single.length())),
                    isTemporary = false
                )
            )
            return true
        }

        return false
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
                        batchFiles = listOf(PickedFile(file.absolutePath, file.name, file.length())),
                        isTemporary = false
                    )
                )
            } else if (file.isDirectory) {
                val errorMsg = when (language) {
                    AppLanguage.PERSIAN -> "پوشه «${file.name}» قابل تحلیل نیست؛ لطفاً یک فایل منفرد را برای محاسبه هش انتخاب کنید."
                    AppLanguage.RUSSIAN -> "Каталог «${file.name}» не поддерживается. Перетащите отдельный файл для хеشیрования."
                    AppLanguage.ENGLISH -> "Cannot analyze folder '${file.name}'. Please drop a single file to calculate its hash."
                }
                onPayloadReady(SharedPayload(errorMessage = errorMsg))
            }
        } else {
            onPayloadReady(SharedPayload(textOrUrl = arg))
        }
    }
}
