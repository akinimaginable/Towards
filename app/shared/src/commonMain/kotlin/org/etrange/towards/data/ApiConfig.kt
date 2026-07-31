package org.etrange.towards.data

data class ApiConfig(
    val baseUrl: String,
)

expect fun defaultApiBaseUrl(): String
