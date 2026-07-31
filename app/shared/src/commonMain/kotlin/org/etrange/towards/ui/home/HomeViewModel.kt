package org.etrange.towards.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.etrange.towards.data.ApiException
import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.port.Geocoder

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val geocoder: Geocoder,
) : ViewModel() {
    private val _destination = MutableStateFlow("")
    val destination: StateFlow<String> = _destination.asStateFlow()

    private val _suggestions = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val suggestions: StateFlow<List<GeocodeResult>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selected = MutableStateFlow<GeocodeResult?>(null)
    val selected: StateFlow<GeocodeResult?> = _selected.asStateFlow()

    private val _shortcuts = MutableStateFlow(
        listOf(
            DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
            DestinationShortcutItem(label = "Work", detail = "17 min"),
            DestinationShortcutItem(label = "School", detail = "47 min"),
            DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
        ),
    )
    val shortcuts: StateFlow<List<DestinationShortcutItem>> = _shortcuts.asStateFlow()

    private var searchJob: Job? = null

    init {
        _destination
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query -> search(query) }
            .launchIn(viewModelScope)
    }

    fun onDestinationChange(value: String) {
        _destination.value = value
        _selected.value = null
        if (value.isBlank()) {
            _suggestions.value = emptyList()
            _errorMessage.value = null
            _isLoading.value = false
        }
    }

    fun onShortcutClick(shortcut: DestinationShortcutItem) {
        _destination.value = shortcut.label
        _selected.value = null
    }

    fun onSuggestionClick(result: GeocodeResult) {
        _destination.value = result.name
        _selected.value = result
        _suggestions.value = emptyList()
        _errorMessage.value = null
    }

    private fun search(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _suggestions.value = emptyList()
            _errorMessage.value = null
            _isLoading.value = false
            return
        }
        // Skip re-search when the field was filled from a selection.
        if (_selected.value?.name == trimmed) {
            return
        }

        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _suggestions.value = geocoder.geocode(
                    GeocodeRequest(
                        text = trimmed,
                        numberOfResults = 10,
                    ),
                )
                _isLoading.value = false
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                _suggestions.value = emptyList()
                _errorMessage.value = error.message
                _isLoading.value = false
            } catch (error: Exception) {
                _suggestions.value = emptyList()
                _errorMessage.value = error.message ?: "Search failed"
                _isLoading.value = false
            }
        }
    }
}
