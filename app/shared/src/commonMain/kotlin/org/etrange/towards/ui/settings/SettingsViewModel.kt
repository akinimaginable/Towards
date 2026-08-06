package org.etrange.towards.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.etrange.towards.data.ApiEndpointStore
import org.etrange.towards.ui.theme.ThemeMode

class SettingsViewModel(
    val endpointStore: ApiEndpointStore = ApiEndpointStore(),
) : ViewModel() {
    private val _themeMode = MutableStateFlow(ThemeMode.System)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    val apiEndpoint: StateFlow<String> = endpointStore.endpoint

    private val _apiEndpointDraft = MutableStateFlow(endpointStore.endpoint.value)
    val apiEndpointDraft: StateFlow<String> = _apiEndpointDraft.asStateFlow()

    private val _apiEndpointError = MutableStateFlow<String?>(null)
    val apiEndpointError: StateFlow<String?> = _apiEndpointError.asStateFlow()

    fun onThemeModeChange(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun onApiEndpointDraftChange(value: String) {
        _apiEndpointDraft.value = value
        _apiEndpointError.value = null
    }

    fun onApiEndpointSave(): Boolean {
        val result = endpointStore.save(_apiEndpointDraft.value)
        return result.fold(
            onSuccess = { saved ->
                _apiEndpointDraft.value = saved
                _apiEndpointError.value = null
                true
            },
            onFailure = { error ->
                _apiEndpointError.value = error.message ?: ApiEndpointStore.INVALID_ENDPOINT_MESSAGE
                false
            },
        )
    }
}
