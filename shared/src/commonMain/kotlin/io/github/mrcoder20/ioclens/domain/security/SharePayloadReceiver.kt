package io.github.mrcoder20.ioclens.domain.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SharedPayload(
    val textOrUrl: String? = null,
    val filePath: String? = null,
    val fileName: String? = null
)

object SharePayloadReceiver {
    private val _incomingPayload = MutableStateFlow<SharedPayload?>(null)
    val incomingPayload: StateFlow<SharedPayload?> = _incomingPayload.asStateFlow()

    fun dispatch(payload: SharedPayload) {
        _incomingPayload.value = payload
    }

    fun consume(): SharedPayload? {
        val current = _incomingPayload.value
        _incomingPayload.value = null
        return current
    }
}
