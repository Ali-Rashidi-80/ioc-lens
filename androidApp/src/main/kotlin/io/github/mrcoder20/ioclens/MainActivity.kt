package io.github.mrcoder20.ioclens

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import io.github.mrcoder20.ioclens.core.components.PickedFile
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocaleManager
import io.github.mrcoder20.ioclens.core.storage.AndroidStorageContext
import io.github.mrcoder20.ioclens.domain.security.DefaultTemporaryFileCleaner
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val MAX_SHARED_INGEST_BYTES = 256L * 1024L * 1024L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidStorageContext.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            DefaultTemporaryFileCleaner.sweepStaleTempFiles()
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleShareIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action

        when (action) {
            Intent.ACTION_SEND -> {
                val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }

                if (streamUri != null) {
                    copyUriToCacheAndDispatchAsync(streamUri)
                    return
                }

                val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    SharePayloadReceiver.dispatch(SharedPayload(textOrUrl = sharedText))
                    return
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val streamUris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!streamUris.isNullOrEmpty()) {
                    copyMultipleUrisToCacheAndDispatchAsync(streamUris)
                    return
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri = intent.data
                if (dataUri != null) {
                    val uriString = dataUri.toString()
                    when {
                        dataUri.scheme.equals("ioclens", ignoreCase = true) -> {
                            val cleanUrl = uriString.removePrefix("ioclens://")
                            if (cleanUrl.isNotBlank()) {
                                SharePayloadReceiver.dispatch(SharedPayload(textOrUrl = cleanUrl))
                            }
                        }
                        dataUri.scheme.equals("content", ignoreCase = true) -> {
                            copyUriToCacheAndDispatchAsync(dataUri)
                        }
                        else -> {
                            logDiagnostic("MainActivity", "Rejected unsupported VIEW URI scheme: ${dataUri.scheme}")
                        }
                    }
                }
            }
        }
    }

    private fun localizedShareError(exceededLimit: Boolean): String {
        val lang = LocaleManager.currentLanguage.value
        return if (exceededLimit) {
            when (lang) {
                AppLanguage.PERSIAN -> "حجم فایل اشتراک‌گذاری‌شده از حد مجاز ۲۵۶ مگابایت بیشتر است."
                AppLanguage.RUSSIAN -> "Размер переданного файла превышает лимит 256 МБ."
                AppLanguage.ENGLISH -> "Shared file exceeds the 256 MB client-side hashing limit."
            }
        } else {
            when (lang) {
                AppLanguage.PERSIAN -> "خواندن فایل اشتراک‌گذاری‌شده ناموفق بود یا دسترسی مجاز نیست."
                AppLanguage.RUSSIAN -> "Не удалось прочитать переданный файл или доступ запрещён."
                AppLanguage.ENGLISH -> "Unable to read shared payload or URI access was rejected."
            }
        }
    }

    private fun sanitizeDisplayName(rawName: String?, fallback: String): String {
        val sanitized = rawName.orEmpty()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('.')
            .take(100)
        return sanitized.ifBlank { fallback }
    }

    private fun isSafeExternalContentUri(uri: Uri): Boolean {
        if (!uri.scheme.equals("content", ignoreCase = true)) return false
        val rawPath = uri.path ?: ""
        val pkg = applicationContext.packageName
        if (rawPath.contains("/data/data/$pkg") || rawPath.contains("/data/user/0/$pkg")) {
            return false
        }
        val appDataRoot = applicationInfo.dataDir
        if (!appDataRoot.isNullOrBlank() && rawPath.contains(appDataRoot)) {
            return false
        }
        return true
    }

    private fun copyContentUriBlocking(uri: Uri, prefix: String, fallbackName: String): PickedFile {
        if (!isSafeExternalContentUri(uri)) {
            throw SecurityException("Rejected unsafe or non-content URI scheme: ${uri.scheme}")
        }
        val contentResolver = applicationContext.contentResolver
        var rawDisplayName: String? = null
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (colIdx != -1) {
                        rawDisplayName = cursor.getString(colIdx)
                    }
                }
            }
        }.onFailure { err ->
            logDiagnostic("MainActivity", "DISPLAY_NAME query failed, falling back to segment", err)
            rawDisplayName = uri.lastPathSegment
        }

        val displayName = sanitizeDisplayName(rawDisplayName ?: uri.lastPathSegment, fallbackName)
        val tempFile = File.createTempFile(prefix, ".tmp", cacheDir)
        var totalWritten = 0L
        try {
            val input = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("ContentResolver returned null InputStream")
            input.use { stream ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        totalWritten += read
                        if (totalWritten > MAX_SHARED_INGEST_BYTES) {
                            throw IllegalArgumentException("Payload size exceeds $MAX_SHARED_INGEST_BYTES bytes")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            return PickedFile(path = tempFile.absolutePath, name = displayName, sizeBytes = totalWritten)
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }

    private fun copyUriToCacheAndDispatchAsync(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val copied = copyContentUriBlocking(uri, prefix = "share_", fallbackName = "shared_payload.bin")
                SharePayloadReceiver.dispatch(
                    SharedPayload(
                        filePath = copied.path,
                        fileName = copied.name,
                        batchFiles = listOf(copied),
                        isTemporary = true
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logDiagnostic("MainActivity", "Failed to ingest shared URI", e)
                val isLimitError = e is IllegalArgumentException && e.message?.contains("exceeds") == true
                SharePayloadReceiver.dispatch(
                    SharedPayload(errorMessage = localizedShareError(exceededLimit = isLimitError))
                )
            }
        }
    }

    private fun copyMultipleUrisToCacheAndDispatchAsync(uris: List<Uri>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val copiedFiles = mutableListOf<PickedFile>()
            try {
                uris.forEachIndexed { idx, uri ->
                    val copied = copyContentUriBlocking(
                        uri = uri,
                        prefix = "batch_",
                        fallbackName = "shared_payload_$idx.bin"
                    )
                    copiedFiles.add(copied)
                }
                if (copiedFiles.isNotEmpty()) {
                    val first = copiedFiles.first()
                    SharePayloadReceiver.dispatch(
                        SharedPayload(
                            filePath = first.path,
                            fileName = first.name,
                            batchFiles = copiedFiles,
                            isTemporary = true
                        )
                    )
                }
            } catch (e: CancellationException) {
                copiedFiles.forEach { DefaultTemporaryFileCleaner.cleanIfTemporary(it.path) }
                throw e
            } catch (e: Exception) {
                logDiagnostic("MainActivity", "Failed to ingest multi-stream shared URIs", e)
                copiedFiles.forEach { DefaultTemporaryFileCleaner.cleanIfTemporary(it.path) }
                val isLimitError = e is IllegalArgumentException && e.message?.contains("exceeds") == true
                SharePayloadReceiver.dispatch(
                    SharedPayload(errorMessage = localizedShareError(exceededLimit = isLimitError))
                )
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
