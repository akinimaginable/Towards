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
import org.etrange.towards.data.LocationBiasStore
import org.etrange.towards.data.LocationProvider
import org.etrange.towards.data.toApiDateTime
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.StopTimesRequest
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.domain.port.TimetableProvider
import org.etrange.towards.ui.trip.TripEndpoint
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val geocoder: Geocoder,
    private val locationProvider: LocationProvider,
    private val timetableProvider: TimetableProvider,
    private val locationBiasStore: LocationBiasStore = LocationBiasStore(),
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

    private val _origin = MutableStateFlow(TripEndpoint("My location", MAP_CENTER))
    val origin: StateFlow<TripEndpoint> = _origin.asStateFlow()

    private var followUserLocation = true

    private val _shortcuts = MutableStateFlow(
        listOf(
            DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
            DestinationShortcutItem(label = "Work", detail = "17 min"),
            DestinationShortcutItem(label = "School", detail = "47 min"),
            DestinationShortcutItem(
                label = "Grand Place",
                detail = "7 min",
                coordinate = Coordinate(50.8467, 4.3525),
            ),
        ),
    )
    val shortcuts: StateFlow<List<DestinationShortcutItem>> = _shortcuts.asStateFlow()

    private var searchJob: Job? = null
    private var locateJob: Job? = null
    private var nearbyJob: Job? = null
    private var nearbyPollJob: Job? = null

    init {
        _destination
            .debounce(300.milliseconds)
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
        seedLocationBias()
        refreshLocationBias()
        if (_locationBias.value == null && !locationProvider.hasPermission()) {
            _nearbyMessage.value = "Turn on location to see nearby departures"
        }
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

    fun routingOrigin(): TripEndpoint = _origin.value

    fun setOrigin(endpoint: TripEndpoint) {
        followUserLocation = endpoint.isCurrentLocation()
        _origin.value = endpoint
        _errorMessage.value = null
    }

    fun onSamePlaceSelected() {
        _errorMessage.value = "Choose a different destination"
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
                applyLocationBias(coordinate)
                followUserLocation = true
                _origin.value = TripEndpoint(name = "My location", coordinate = coordinate)
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
        val lastKnown = locationProvider.lastKnownCoordinate()
        if (lastKnown != null) {
            applyLocationBias(lastKnown)
        } else if (_locationBias.value == null) {
            _isLoadingNearby.value = true
        }
        refreshLocationBias()
    }

    private fun seedLocationBias() {
        locationBiasStore.load()?.let { _locationBias.value = it }
        if (!locationProvider.hasPermission()) return
        locationProvider.lastKnownCoordinate()?.let { applyLocationBias(it) }
    }

    private fun refreshLocationBias() {
        if (!locationProvider.hasPermission()) return
        viewModelScope.launch {
            val coordinate = runCatching { locationProvider.currentCoordinate() }.getOrNull()
            if (coordinate != null) {
                applyLocationBias(coordinate)
            } else if (_locationBias.value == null) {
                _isLoadingNearby.value = false
                _nearbyMessage.value = "Unable to determine your current location"
            }
        }
    }

    private fun applyLocationBias(coordinate: Coordinate) {
        _locationBias.value = coordinate
        locationBiasStore.save(coordinate)
        if (followUserLocation) {
            _origin.value = TripEndpoint(name = "My location", coordinate = coordinate)
        }
    }

    private fun startNearbyPolling() {
        nearbyPollJob?.cancel()
        nearbyPollJob = viewModelScope.launch {
            while (isActive) {
                delay(NEARBY_POLL_INTERVAL_MS.milliseconds)
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
                val now = Clock.System.now()
                val stopTimes = timetableProvider.getStopTimes(
                    StopTimesRequest(
                        center = coordinate,
                        radiusMeters = NEARBY_RADIUS_METERS,
                        numberOfEvents = NEARBY_EVENT_COUNT,
                        time = now.toApiDateTime(),
                    ),
                )
                _nearbyStops.value = groupNearbyDepartures(
                    stopTimes,
                    coordinate,
                    maxStops = NEARBY_MAX_STOPS,
                    now = now,
                )
                _nearbyMessage.value = if (_nearbyStops.value.isEmpty()) {
                    "No departures within ${NEARBY_RADIUS_METERS} m of your location"
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
        private val MAP_CENTER = Coordinate(latitude = 50.8503, longitude = 4.3517)
        private const val NEARBY_RADIUS_METERS = 1_500
        private const val NEARBY_MAX_STOPS = 20
        private const val NEARBY_EVENT_COUNT = 250
        private const val NEARBY_POLL_INTERVAL_MS = 60_000L
    }
}

private fun TripEndpoint.isCurrentLocation(): Boolean =
    stopId == null && (name == "My location" || name == "Map center")
