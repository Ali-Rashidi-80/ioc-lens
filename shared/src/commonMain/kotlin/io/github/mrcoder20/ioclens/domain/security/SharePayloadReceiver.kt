package io.github.mrcoder20.ioclens.domain.security

import io.github.mrcoder20.ioclens.core.components.PickedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

data class SharedPayload(
    val textOrUrl: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val batchFiles: List<PickedFile>? = null,
    val isTemporary: Boolean = false,
    val errorMessage: String? = null
)

object SharePayloadReceiver {
    private val _incomingPayload = MutableStateFlow<SharedPayload?>(null)
    val incomingPayload: StateFlow<SharedPayload?> = _incomingPayload.asStateFlow()

    fun dispatch(payload: SharedPayload) {
        val previous = _incomingPayload.getAndUpdate { payload }
        if (previous != null && previous !== payload) {
            cleanDroppedPayload(previous)
        }
    }

    fun consume(): SharedPayload? {
        return _incomingPayload.getAndUpdate { null }
    }

    private fun cleanDroppedPayload(payload: SharedPayload) {
        if (!payload.isTemporary) return
        payload.filePath?.let { DefaultTemporaryFileCleaner.cleanIfTemporary(it) }
        payload.batchFiles?.forEach { file ->
            DefaultTemporaryFileCleaner.cleanIfTemporary(file.path)
        }
    }
}
