package org.etrange.towards.ui.trip

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
import org.etrange.towards.data.LocationBiasStore
import org.etrange.towards.data.LocationProvider
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.ui.home.DestinationShortcutItem
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class LocationPickerViewModel(
    private val geocoder: Geocoder,
    private val locationProvider: LocationProvider,
    private val locationBiasStore: LocationBiasStore = LocationBiasStore(),
    private val otherPlace: TripEndpoint? = null,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val suggestions: StateFlow<List<GeocodeResult>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val shortcuts: List<DestinationShortcutItem> = emptyList()

    private val locationBias: Coordinate? = locationBiasStore.load()
        ?: locationProvider.lastKnownCoordinate()

    private var searchJob: Job? = null
    private var locateJob: Job? = null

    init {
        _query
            .debounce(300.milliseconds)
            .distinctUntilChanged()
            .onEach { query -> search(query) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) {
        _query.value = value
        _errorMessage.value = null
        if (value.isBlank()) {
            _suggestions.value = emptyList()
            _isLoading.value = false
        }
    }

    fun onShortcutClick(shortcut: DestinationShortcutItem): TripEndpoint? {
        val coordinate = shortcut.coordinate ?: run {
            _query.value = shortcut.label
            return null
        }
        return accept(
            TripEndpoint(
                name = shortcut.label,
                coordinate = coordinate,
                stopId = shortcut.stopId,
            ),
        )
    }

    fun onSuggestionClick(result: GeocodeResult): TripEndpoint? =
        accept(result.toTripEndpoint())

    fun onMyLocationClick(onResult: (TripEndpoint?) -> Unit) {
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            _isLocating.value = true
            _errorMessage.value = null
            try {
                val coordinate = locationProvider.currentCoordinate()
                    ?: locationProvider.lastKnownCoordinate()
                    ?: locationBiasStore.load()
                if (coordinate == null) {
                    _errorMessage.value = "Unable to determine your current location"
                    onResult(null)
                    return@launch
                }
                onResult(accept(TripEndpoint(name = "My location", coordinate = coordinate)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "Unable to determine your current location"
                onResult(null)
            } finally {
                _isLocating.value = false
            }
        }
    }

    private fun accept(endpoint: TripEndpoint): TripEndpoint? {
        val other = otherPlace
        if (other != null && other.isSamePlace(endpoint)) {
            _errorMessage.value = "Choose a different place"
            return null
        }
        return endpoint
    }

    private fun search(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _suggestions.value = emptyList()
            _isLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                _suggestions.value = geocoder.geocode(
                    GeocodeRequest(
                        text = trimmed,
                        bias = locationBias,
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
                _isLoading.value = false
            }
        }
    }
}
