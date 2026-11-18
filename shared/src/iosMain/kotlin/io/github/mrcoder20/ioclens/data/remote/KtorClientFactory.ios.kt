package io.github.mrcoder20.ioclens.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformHttpClient(timeoutMs: Long): HttpClient {
    val boundedTimeout = timeoutMs.coerceIn(1_000L, 60_000L)
    return HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
        HttpClientConfigApplyDefaults(this, boundedTimeout)
    }
}
