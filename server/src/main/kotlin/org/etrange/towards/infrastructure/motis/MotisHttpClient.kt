package org.etrange.towards.infrastructure.motis

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createMotisHttpClient(
    engineFactory: HttpClientEngineFactory<*>,
    json: Json,
    requestTimeoutMillis: Long,
): HttpClient = HttpClient(engineFactory) {
    configureMotis(json, requestTimeoutMillis)
}

internal fun HttpClientConfig<*>.configureMotis(
    json: Json,
    timeoutMillis: Long,
) {
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = timeoutMillis
        connectTimeoutMillis = timeoutMillis
        socketTimeoutMillis = timeoutMillis
    }
}
