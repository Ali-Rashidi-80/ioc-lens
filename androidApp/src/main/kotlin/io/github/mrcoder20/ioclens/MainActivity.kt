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
import io.github.mrcoder20.ioclens.domain.security.SharePayloadReceiver
import io.github.mrcoder20.ioclens.domain.security.SharedPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

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
        val type = intent.type

        when (action) {
            Intent.ACTION_SEND -> {
                // If a stream URI is present (file/image from Telegram, Eitaa, WhatsApp, etc.),
                // prioritize the file stream even if a caption is present in EXTRA_TEXT.
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
            Intent.ACTION_VIEW -> {
                val dataUri = intent.data
                if (dataUri != null) {
                    val uriString = dataUri.toString()
                    if (uriString.startsWith("http://") || uriString.startsWith("https://") || uriString.startsWith("ioclens://")) {
                        val cleanUrl = if (uriString.startsWith("ioclens://")) uriString.removePrefix("ioclens://") else uriString
                        SharePayloadReceiver.dispatch(SharedPayload(textOrUrl = cleanUrl))
                    } else if (dataUri.scheme == "content" || dataUri.scheme == "file") {
                        copyUriToCacheAndDispatchAsync(dataUri)
                    }
                }
            }
        }
    }

    private fun copyUriToCacheAndDispatchAsync(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = applicationContext.contentResolver
                var displayName = "shared_payload.bin"

                runCatching {
                    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val colIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (colIdx != -1) {
                                displayName = cursor.getString(colIdx) ?: displayName
                            }
                        }
                    }
                }.onFailure {
                    displayName = uri.lastPathSegment ?: displayName
                }

                val tempFile = File(cacheDir, "shared_stream_${System.currentTimeMillis()}_$displayName")

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                    }
                }

                if (tempFile.exists()) {
                    SharePayloadReceiver.dispatch(
                        SharedPayload(
                            filePath = tempFile.absolutePath,
                            fileName = displayName
                        )
                    )
                }
            } catch (e: Exception) {
                // Surface error if any
                SharePayloadReceiver.dispatch(
                    SharedPayload(
                        textOrUrl = null,
                        filePath = null,
                        fileName = null
                    )
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
