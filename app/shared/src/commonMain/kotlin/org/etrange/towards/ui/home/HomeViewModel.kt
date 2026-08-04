package org.etrange.towards.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.etrange.towards.data.ApiException
import org.etrange.towards.data.LocationProvider
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.ReverseGeocodeRequest
import org.etrange.towards.domain.model.StopTimesRequest
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.domain.port.TimetableProvider

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val geocoder: Geocoder,
    private val locationProvider: LocationProvider,
    private val timetableProvider: TimetableProvider,
) : ViewModel() {
    private val _destination = MutableStateFlow("")
    val destination: StateFlow<String> = _destination.asStateFlow()

    private val _suggestions = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val suggestions: StateFlow<List<GeocodeResult>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selected = MutableStateFlow<GeocodeResult?>(null)
    val selected: StateFlow<GeocodeResult?> = _selected.asStateFlow()

    private val _locationBias = MutableStateFlow<Coordinate?>(null)
    val locationBias: StateFlow<Coordinate?> = _locationBias.asStateFlow()

    private val _nearbyStops = MutableStateFlow<List<NearbyStop>>(emptyList())
    val nearbyStops: StateFlow<List<NearbyStop>> = _nearbyStops.asStateFlow()

    private val _isLoadingNearby = MutableStateFlow(false)
    val isLoadingNearby: StateFlow<Boolean> = _isLoadingNearby.asStateFlow()

    private val _nearbyMessage = MutableStateFlow<String?>(null)
    val nearbyMessage: StateFlow<String?> = _nearbyMessage.asStateFlow()

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
    private var locateJob: Job? = null
    private var nearbyJob: Job? = null
    private var nearbyPollJob: Job? = null

    init {
        _destination
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query -> search(query) }
            .launchIn(viewModelScope)

        _locationBias
            .onEach { coordinate ->
                if (coordinate != null && _destination.value.isBlank()) {
                    loadNearbyDepartures(coordinate)
                }
            }
            .launchIn(viewModelScope)

        startNearbyPolling()
        refreshLocationBias()
    }

    fun hasLocationPermission(): Boolean = locationProvider.hasPermission()

    fun onDestinationChange(value: String) {
        _destination.value = value
        _selected.value = null
        if (value.isBlank()) {
            _suggestions.value = emptyList()
            _errorMessage.value = null
            _isLoading.value = false
            _locationBias.value?.let { loadNearbyDepartures(it) }
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

    fun onUseCurrentLocation() {
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            _isLocating.value = true
            _errorMessage.value = null
            try {
                val coordinate = locationProvider.currentCoordinate()
                if (coordinate == null) {
                    _errorMessage.value = "Unable to determine your current location"
                    return@launch
                }
                _locationBias.value = coordinate
                val results = geocoder.reverseGeocode(
                    ReverseGeocodeRequest(
                        coordinate = coordinate,
                        numberOfResults = 1,
                    ),
                )
                val match = results.firstOrNull()
                if (match == null) {
                    _destination.value = "${coordinate.latitude}, ${coordinate.longitude}"
                    _selected.value = GeocodeResult(
                        id = "current:${coordinate.latitude},${coordinate.longitude}",
                        kind = LocationKind.PLACE,
                        name = "Current location",
                        coordinate = coordinate,
                    )
                    _suggestions.value = emptyList()
                } else {
                    onSuggestionClick(match)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                _errorMessage.value = error.message
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "Unable to determine your current location"
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun onLocationPermissionDenied(fromUserAction: Boolean = true) {
        if (fromUserAction) {
            _errorMessage.value = "Location permission is required to use your current position"
        } else {
            _nearbyMessage.value = "Turn on location to see nearby departures"
        }
    }

    fun onLocationPermissionGranted() {
        _nearbyMessage.value = null
        refreshLocationBias()
    }

    private fun refreshLocationBias() {
        if (!locationProvider.hasPermission()) return
        viewModelScope.launch {
            runCatching { locationProvider.currentCoordinate() }
                .getOrNull()
                ?.let { _locationBias.value = it }
        }
    }

    private fun startNearbyPolling() {
        nearbyPollJob?.cancel()
        nearbyPollJob = viewModelScope.launch {
            while (isActive) {
                delay(NEARBY_POLL_INTERVAL_MS)
                if (_destination.value.isBlank()) {
                    val coordinate = _locationBias.value ?: continue
                    loadNearbyDepartures(coordinate)
                }
            }
        }
    }

    private fun loadNearbyDepartures(coordinate: Coordinate) {
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            _isLoadingNearby.value = true
            try {
                val stopTimes = timetableProvider.getStopTimes(
                    StopTimesRequest(
                        center = coordinate,
                        radiusMeters = NEARBY_RADIUS_METERS,
                        numberOfEvents = NEARBY_EVENT_COUNT,
                    ),
                )
                _nearbyStops.value = groupNearbyDepartures(stopTimes, coordinate)
                _nearbyMessage.value = if (_nearbyStops.value.isEmpty()) {
                    "No departures nearby"
                } else {
                    null
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                _nearbyMessage.value = error.message
            } catch (error: Exception) {
                _nearbyMessage.value = error.message ?: "Unable to load nearby departures"
            } finally {
                _isLoadingNearby.value = false
            }
        }
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
                        bias = _locationBias.value,
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

    companion object {
        private const val NEARBY_RADIUS_METERS = 500
        private const val NEARBY_EVENT_COUNT = 40
        private const val NEARBY_POLL_INTERVAL_MS = 60_000L
    }
}
