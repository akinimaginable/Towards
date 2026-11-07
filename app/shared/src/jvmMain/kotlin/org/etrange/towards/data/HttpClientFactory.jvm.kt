package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.compression.ContentEncoding

actual fun createHttpClient(): HttpClient = createHttpClient(CIO) {
    install(ContentEncoding) {
        gzip()
    }
}
