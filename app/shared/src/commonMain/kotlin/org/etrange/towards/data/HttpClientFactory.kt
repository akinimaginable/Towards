package org.etrange.towards.data

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
