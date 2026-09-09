package com.congnguyencn.kmpstreamtv.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * Small Ktor boundary shared by Android and iOS.
 *
 * Home intentionally uses local fixtures today. Keeping HTTP behind this type means a real Home
 * data source can replace the dummy one without leaking Ktor response types into domain or UI code.
 */
class StreamTvApiClient internal constructor(private val httpClient: HttpClient) {
    suspend fun getText(path: String): String = httpClient.get(path).bodyAsText()
}

internal expect fun platformHttpClient(): HttpClient

internal fun createStreamTvHttpClient(): HttpClient = platformHttpClient().config {
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
    defaultRequest {
        headers.append("Accept", "application/json")
    }
}
