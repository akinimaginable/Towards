package org.etrange.towards.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.etrange.towards.data.SettingsStore
import org.etrange.towards.ui.theme.ThemeMode

class SettingsViewModel(val settingsStore: SettingsStore = SettingsStore()) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
    val apiEndpoint: StateFlow<String> = settingsStore.endpoint

    private val _apiEndpointDraft = MutableStateFlow(settingsStore.endpoint.value)
    val apiEndpointDraft: StateFlow<String> = _apiEndpointDraft.asStateFlow()

    private val _apiEndpointError = MutableStateFlow<String?>(null)
    val apiEndpointError: StateFlow<String?> = _apiEndpointError.asStateFlow()

    fun onThemeModeChange(mode: ThemeMode) {
        settingsStore.saveThemeMode(mode)
    }

    fun onApiEndpointDraftChange(value: String) {
        _apiEndpointDraft.value = value
        _apiEndpointError.value = null
    }

    fun onApiEndpointSave(): Boolean {
        val result = settingsStore.save(_apiEndpointDraft.value)
        return result.fold(
            onSuccess = { saved ->
                _apiEndpointDraft.value = saved
                _apiEndpointError.value = null
                true
            },
            onFailure = { error ->
                _apiEndpointError.value = error.message ?: SettingsStore.INVALID_ENDPOINT_MESSAGE
                false
            }
        )
    }
}
