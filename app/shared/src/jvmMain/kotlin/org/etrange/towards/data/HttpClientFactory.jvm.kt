package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createHttpClient(): HttpClient = createHttpClient(CIO)
