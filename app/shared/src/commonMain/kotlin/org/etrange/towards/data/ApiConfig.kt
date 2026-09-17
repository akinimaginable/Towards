package org.etrange.towards.data

class ApiConfig(private val baseUrlProvider: () -> String) {
    val baseUrl: String
        get() = baseUrlProvider()

    constructor(baseUrl: String) : this({ baseUrl })

    constructor(endpointStore: SettingsStore) : this({ endpointStore.endpoint.value })
}

expect fun defaultApiBaseUrl(): String
