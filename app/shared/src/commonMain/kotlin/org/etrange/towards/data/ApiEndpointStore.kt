package org.etrange.towards.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists and exposes the Towards API base URL.
 * Falls back to [defaultApiBaseUrl] when nothing has been saved yet.
 */
class ApiEndpointStore(
    private val settings: Settings = Settings(),
    private val defaultUrl: String = defaultApiBaseUrl(),
) {
    private val _endpoint = MutableStateFlow(loadPersistedOrDefault())
    val endpoint: StateFlow<String> = _endpoint.asStateFlow()

    fun save(raw: String): Result<String> {
        val normalized = validateApiEndpoint(raw)
            ?: return Result.failure(IllegalArgumentException(INVALID_ENDPOINT_MESSAGE))
        settings[KEY_API_ENDPOINT] = normalized
        _endpoint.value = normalized
        return Result.success(normalized)
    }

    private fun loadPersistedOrDefault(): String {
        val saved = settings.getStringOrNull(KEY_API_ENDPOINT) ?: return normalizeApiEndpoint(defaultUrl)
        return validateApiEndpoint(saved) ?: normalizeApiEndpoint(defaultUrl)
    }

    companion object {
        const val KEY_API_ENDPOINT = "api_endpoint"
        const val INVALID_ENDPOINT_MESSAGE =
            "Enter a valid http or https URL with a host, for example http://127.0.0.1:8081"
    }
}
