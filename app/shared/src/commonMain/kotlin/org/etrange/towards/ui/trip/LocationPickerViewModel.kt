package org.etrange.towards.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.etrange.towards.data.LocationBiasStore
import org.etrange.towards.data.LocationProvider
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.search.PlaceSearch

class LocationPickerViewModel(
    private val geocoder: Geocoder,
    private val locationProvider: LocationProvider,
    private val locationBiasStore: LocationBiasStore = LocationBiasStore(),
    private val otherPlace: TripEndpoint? = null,
) : ViewModel() {
    private val locationBias: Coordinate? =
        locationBiasStore.load() ?: locationProvider.lastKnownCoordinate()

    private val search = PlaceSearch(
        geocoder = geocoder,
        scope = viewModelScope,
        bias = { locationBias },
    )
    val query: StateFlow<String> = search.query
    val suggestions: StateFlow<List<GeocodeResult>> = search.suggestions
    val isLoading: StateFlow<Boolean> = search.isLoading

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _localError = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> =
        combine(_localError, search.errorMessage) { local, searchError -> local ?: searchError }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val shortcuts: List<DestinationShortcutItem> = emptyList()

    private var locateJob: Job? = null

    fun onQueryChange(value: String) {
        _localError.value = null
        search.onQueryChange(value)
    }

    fun onShortcutClick(shortcut: DestinationShortcutItem): TripEndpoint? {
        val coordinate = shortcut.coordinate ?: run {
            onQueryChange(shortcut.label)
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

    fun onSuggestionClick(result: GeocodeResult): TripEndpoint? = accept(result.toTripEndpoint())

    fun onMyLocationClick(onResult: (TripEndpoint?) -> Unit) {
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            _isLocating.value = true
            _localError.value = null
            search.clearError()
            try {
                val coordinate =
                    locationProvider.currentCoordinate() ?: locationProvider.lastKnownCoordinate()
                    ?: locationBiasStore.load()
                if (coordinate == null) {
                    _localError.value = "Unable to determine your current location"
                    onResult(null)
                    return@launch
                }
                onResult(accept(TripEndpoint(name = "My location", coordinate = coordinate)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _localError.value = error.message ?: "Unable to determine your current location"
                onResult(null)
            } finally {
                _isLocating.value = false
            }
        }
    }

    private fun accept(endpoint: TripEndpoint): TripEndpoint? {
        val other = otherPlace
        if (other != null && other.isSamePlace(endpoint)) {
            _localError.value = "Choose a different place"
            return null
        }
        return endpoint
    }

}
