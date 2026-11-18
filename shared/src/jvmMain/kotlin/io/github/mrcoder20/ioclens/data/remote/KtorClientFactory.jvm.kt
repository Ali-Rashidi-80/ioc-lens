package io.github.mrcoder20.ioclens.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import javax.net.ssl.SSLContext

internal actual fun createPlatformHttpClient(timeoutMs: Long): HttpClient {
    val boundedTimeout = timeoutMs.coerceIn(1_000L, 60_000L)
    val connectTimeout = minOf(5_000L, boundedTimeout)
    return HttpClient(CIO) {
        engine {
            maxConnectionsCount = 64
            https {
                trustManager = null
                runCatching {
                    val sslContext = SSLContext.getInstance("TLSv1.3")
                    sslContext.init(null, null, null)
                }
            }
            endpoint {
                maxConnectionsPerRoute = 8
                this.connectTimeout = connectTimeout
                connectAttempts = 2
                keepAliveTime = 5_000L
            }
        }
        HttpClientConfigApplyDefaults(this, boundedTimeout)
    }
}
