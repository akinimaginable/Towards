package org.etrange.towards.data

/**
 * Resolves the Towards API base URL for each request so settings changes apply immediately.
 */
class ApiConfig(
    private val baseUrlProvider: () -> String,
) {
    val baseUrl: String
        get() = baseUrlProvider()

    constructor(baseUrl: String) : this({ baseUrl })

    constructor(endpointStore: ApiEndpointStore) : this({ endpointStore.endpoint.value })
}

expect fun defaultApiBaseUrl(): String
