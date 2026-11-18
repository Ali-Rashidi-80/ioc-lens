package io.github.mrcoder20.ioclens.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

internal expect fun createPlatformHttpClient(timeoutMs: Long): HttpClient

fun createZeroKeyHttpClient(timeoutMs: Long = 10_000L): HttpClient {
    return createPlatformHttpClient(timeoutMs)
}

internal fun HttpClientConfigApplyDefaults(
    config: io.ktor.client.HttpClientConfig<*>,
    timeoutMs: Long
) {
    val boundedTimeout = timeoutMs.coerceIn(1_000L, 60_000L)
    val connectTimeout = minOf(5_000L, boundedTimeout)
    with(config) {
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = boundedTimeout
            connectTimeoutMillis = connectTimeout
            socketTimeoutMillis = boundedTimeout
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "IOCLens/1.0 (Privacy-First SOC Triage)")
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
        }
    }
}
