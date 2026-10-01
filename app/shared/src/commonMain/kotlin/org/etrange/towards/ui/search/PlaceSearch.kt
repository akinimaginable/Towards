package org.etrange.towards.ui.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.port.Geocoder
import kotlin.time.Duration.Companion.milliseconds

/** Debounced place search shared by the home screen and the location picker. */
@OptIn(FlowPreview::class)
class PlaceSearch(
    private val geocoder: Geocoder,
    private val scope: CoroutineScope,
    private val bias: () -> Coordinate?,
) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val suggestions: StateFlow<List<GeocodeResult>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    init {
        _query.debounce(300.milliseconds).distinctUntilChanged()
            .onEach { search(it) }
            .launchIn(scope)
    }

    fun onQueryChange(value: String) {
        _query.value = value
        _errorMessage.value = null
        if (value.isBlank()) {
            searchJob?.cancel()
            _suggestions.value = emptyList()
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun search(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _suggestions.value = emptyList()
            _isLoading.value = false
            return
        }
        val job = scope.launch {
            _isLoading.value = true
            try {
                _suggestions.value = geocoder.geocode(
                    GeocodeRequest(
                        text = trimmed,
                        bias = bias(),
                        numberOfResults = 10,
                    ),
                )
                _errorMessage.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                _suggestions.value = emptyList()
                _errorMessage.value = error.message
            } catch (error: Exception) {
                _suggestions.value = emptyList()
                _errorMessage.value = error.message ?: "Search failed"
            } finally {
                // A newer search may already be running; only the latest one owns the spinner.
                if (searchJob == coroutineContext[Job]) {
                    _isLoading.value = false
                }
            }
        }
        searchJob = job
    }
}
