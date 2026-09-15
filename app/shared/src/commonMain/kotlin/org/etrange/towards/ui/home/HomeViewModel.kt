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
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.model.requests.ReverseGeocodeRequest
import org.etrange.towards.domain.model.requests.StopTimesRequest
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

    private val _userLocation = MutableStateFlow<Coordinate?>(null)
    val userLocation: StateFlow<Coordinate?> = _userLocation.asStateFlow()

    private val _mapFocus = MutableStateFlow(MAP_CENTER)
    val mapFocus: StateFlow<Coordinate> = _mapFocus.asStateFlow()

    private val _followMap = MutableStateFlow(true)
    val followMap: StateFlow<Boolean> = _followMap.asStateFlow()

    private val _focusedPlaceLabel = MutableStateFlow<String?>(null)
    val focusedPlaceLabel: StateFlow<String?> = _focusedPlaceLabel.asStateFlow()

    private val _nearbyStops = MutableStateFlow<List<NearbyStop>>(emptyList())
    val nearbyStops: StateFlow<List<NearbyStop>> = _nearbyStops.asStateFlow()

    private val _isLoadingNearby = MutableStateFlow(false)
    val isLoadingNearby: StateFlow<Boolean> = _isLoadingNearby.asStateFlow()

    private val _nearbyMessage = MutableStateFlow<String?>(null)
    val nearbyMessage: StateFlow<String?> = _nearbyMessage.asStateFlow()

    private val _origin = MutableStateFlow(TripEndpoint("Map center", MAP_CENTER))
    val origin: StateFlow<TripEndpoint> = _origin.asStateFlow()

    private val _shortcuts = MutableStateFlow<List<DestinationShortcutItem>>(emptyList())
    val shortcuts: StateFlow<List<DestinationShortcutItem>> = _shortcuts.asStateFlow()

    private var searchJob: Job? = null
    private var locateJob: Job? = null
    private var nearbyJob: Job? = null
    private var nearbyPollJob: Job? = null
    private var reverseGeocodeJob: Job? = null

    init {
        _destination.debounce(300.milliseconds).distinctUntilChanged()
            .onEach { query -> search(query) }.launchIn(viewModelScope)

        _mapFocus.debounce(300.milliseconds).distinctUntilChanged().onEach { coordinate ->
                if (_destination.value.isBlank()) {
                    loadNearbyDepartures(coordinate)
                }
                if (!_followMap.value) {
                    reverseGeocodeFocus(coordinate)
                }
            }.launchIn(viewModelScope)

        startNearbyPolling()
        seedLocationBias()
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
            loadNearbyDepartures(_mapFocus.value)
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
        _origin.value = endpoint
        _errorMessage.value = null
    }

    fun onSamePlaceSelected() {
        _errorMessage.value = "Choose a different destination"
    }

    fun onUserMovedCamera() {
        _followMap.value = false
    }

    fun onMapCameraIdle(coordinate: Coordinate) {
        if (_followMap.value) return
        _locationBias.value = coordinate
        _origin.value = TripEndpoint(name = "Map center", coordinate = coordinate)
        updateMapFocus(coordinate)
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
                clearFocusedPlace()
                _followMap.value = true
                applyLocationBias(coordinate, originName = "My location")
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
        }
    }

    fun onLocationPermissionGranted() {
        val lastKnown = locationProvider.lastKnownCoordinate()
        if (lastKnown != null) {
            applyLocationBias(lastKnown, originName = "Last known location")
        }
        refreshLocationBias()
    }

    private fun seedLocationBias() {
        locationBiasStore.load()?.let { stored ->
            _locationBias.value = stored
            _origin.value = TripEndpoint(name = "Map center", coordinate = stored)
            if (_followMap.value) {
                updateMapFocus(stored, force = true)
            }
        }
        if (!locationProvider.hasPermission()) return
        locationProvider.lastKnownCoordinate()?.let {
            applyLocationBias(it, originName = "Last known location")
        }
    }

    private fun refreshLocationBias() {
        if (!locationProvider.hasPermission()) return
        viewModelScope.launch {
            val coordinate = try {
                locationProvider.currentCoordinate()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (coordinate != null) {
                applyLocationBias(coordinate, originName = "My location")
            }
        }
    }

    private fun applyLocationBias(coordinate: Coordinate, originName: String? = null) {
        _locationBias.value = coordinate
        locationBiasStore.save(coordinate)
        if (originName != null) {
            _userLocation.value = coordinate
            _origin.value = TripEndpoint(name = originName, coordinate = coordinate)
        }
        if (_followMap.value) {
            clearFocusedPlace()
            updateMapFocus(coordinate, force = true)
        }
    }

    private fun updateMapFocus(coordinate: Coordinate, force: Boolean = false) {
        if (!force && !shouldUpdateMapFocus(_mapFocus.value, coordinate)) return
        _mapFocus.value = coordinate
    }

    private fun clearFocusedPlace() {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = null
        _focusedPlaceLabel.value = null
    }

    private fun reverseGeocodeFocus(coordinate: Coordinate) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            try {
                val result = geocoder.reverseGeocode(
                    ReverseGeocodeRequest(
                        coordinate = coordinate,
                        numberOfResults = 1,
                    ),
                ).firstOrNull()
                if (!_followMap.value && result != null) {
                    val label = result.name.takeIf { it.isNotBlank() } ?: return@launch
                    _focusedPlaceLabel.value = label
                    _origin.value = TripEndpoint(name = label, coordinate = coordinate)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Keep "Map center" / previous label; do not surface as a search error.
            }
        }
    }

    private fun startNearbyPolling() {
        nearbyPollJob?.cancel()
        nearbyPollJob = viewModelScope.launch {
            while (isActive) {
                delay(NEARBY_POLL_INTERVAL_MS.milliseconds)
                if (_destination.value.isBlank()) {
                    loadNearbyDepartures(_mapFocus.value)
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
                    "No departures within ${NEARBY_RADIUS_METERS} m of this area"
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
                        bias = _mapFocus.value,
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
        private const val NEARBY_MAX_STOPS = 30
        private const val NEARBY_EVENT_COUNT = 250
        private const val NEARBY_POLL_INTERVAL_MS = 60_000L
    }
}
