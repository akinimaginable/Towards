package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(): HttpClient = createHttpClient(OkHttp)
