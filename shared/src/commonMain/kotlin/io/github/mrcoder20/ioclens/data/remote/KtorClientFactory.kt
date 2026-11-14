package io.github.mrcoder20.ioclens.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

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
        defaultRequest {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 IOCLens/1.0")
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
        }
    }
}
