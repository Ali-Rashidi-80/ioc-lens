package io.github.mrcoder20.ioclens.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout

fun createZeroKeyHttpClient(timeoutMs: Long = 10_000L): HttpClient {
    return HttpClient(CIO) {
        engine {
            maxConnectionsCount = 64
            endpoint {
                maxConnectionsPerRoute = 8
                connectTimeout = 5_000L
                connectAttempts = 2
                keepAliveTime = 5_000L
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = 5_000L
            socketTimeoutMillis = timeoutMs
        }
    }
}
