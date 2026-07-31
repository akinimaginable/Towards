package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClient(): HttpClient = createHttpClient(Darwin)
