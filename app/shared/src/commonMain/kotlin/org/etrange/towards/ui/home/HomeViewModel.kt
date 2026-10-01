package org.etrange.towards.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.etrange.towards.data.ApiException
import org.etrange.towards.data.LocationBiasStore
import org.etrange.towards.data.LocationProvider
import org.etrange.towards.data.toApiDateTime
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.requests.ReverseGeocodeRequest
import org.etrange.towards.domain.model.requests.StopTimesRequest
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.domain.port.TimetableProvider
import org.etrange.towards.ui.search.PlaceSearch
import org.etrange.towards.ui.trip.TripEndpoint
import org.etrange.towards.ui.trip.toTripEndpoint
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val geocoder: Geocoder,
    private val locationProvider: LocationProvider,
    private val timetableProvider: TimetableProvider,
    private val locationBiasStore: LocationBiasStore = LocationBiasStore(),
) : ViewModel() {
    private val _mapFocus = MutableStateFlow(MAP_CENTER)
    val mapFocus: StateFlow<Coordinate> = _mapFocus.asStateFlow()

    private val search = PlaceSearch(
        geocoder = geocoder,
        scope = viewModelScope,
        bias = { _mapFocus.value },
    )
    val destination: StateFlow<String> = search.query
    val suggestions: StateFlow<List<GeocodeResult>> = search.suggestions
    val isLoading: StateFlow<Boolean> = search.isLoading

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _localError = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> =
        combine(_localError, search.errorMessage) { local, searchError -> local ?: searchError }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _searchTarget = MutableStateFlow<HomeSearchTarget?>(null)
    val searchTarget: StateFlow<HomeSearchTarget?> = _searchTarget.asStateFlow()

    private val _locationBias = MutableStateFlow<Coordinate?>(null)
    val locationBias: StateFlow<Coordinate?> = _locationBias.asStateFlow()

    private val _userLocation = MutableStateFlow<Coordinate?>(null)
    val userLocation: StateFlow<Coordinate?> = _userLocation.asStateFlow()

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

    private var locateJob: Job? = null
    private var nearbyJob: Job? = null
    private var nearbyPollJob: Job? = null
    private var reverseGeocodeJob: Job? = null

    init {
        _mapFocus.debounce(300.milliseconds).distinctUntilChanged().onEach { coordinate ->
                if (search.query.value.isBlank()) {
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
        _localError.value = null
        search.onQueryChange(value)
        if (value.isBlank()) {
            loadNearbyDepartures(_mapFocus.value)
        }
    }

    fun openSearch() {
        _searchTarget.value = HomeSearchTarget.Destination
    }

    fun pickOrigin() {
        _searchTarget.value = HomeSearchTarget.Origin
        onDestinationChange("")
    }

    /** Switches the open sheet back to destination search, keeping the query. */
    fun returnToDestinationSearch() {
        if (_searchTarget.value == HomeSearchTarget.Origin) {
            _searchTarget.value = HomeSearchTarget.Destination
        }
    }

    fun searchDestination() {
        _searchTarget.value = HomeSearchTarget.Destination
        onDestinationChange("")
    }

    fun dismissSearch() {
        _searchTarget.value = null
        onDestinationChange("")
    }

    /**
     * Handles a shortcut tap. Returns the trip to plan, or null when the tap only fills the
     * search or changes the origin.
     */
    fun onShortcutSelected(shortcut: DestinationShortcutItem): Pair<TripEndpoint, TripEndpoint>? {
        val coordinate = shortcut.coordinate ?: run {
            onDestinationChange(shortcut.label)
            if (_searchTarget.value == null) openSearch()
            return null
        }
        return acceptPlace(
            TripEndpoint(name = shortcut.label, coordinate = coordinate, stopId = shortcut.stopId),
        )
    }

    /** Handles a suggestion tap. Returns the trip to plan, or null when it sets the origin. */
    fun onSuggestionSelected(result: GeocodeResult): Pair<TripEndpoint, TripEndpoint>? =
        acceptPlace(result.toTripEndpoint())

    /** Handles a nearby stop tap. Returns the trip to plan, or null when it is the same place. */
    fun onNearbyStopSelected(stop: NearbyStop): Pair<TripEndpoint, TripEndpoint>? =
        tripFor(stop.toTripEndpoint())

    private fun acceptPlace(place: TripEndpoint): Pair<TripEndpoint, TripEndpoint>? {
        if (_searchTarget.value == HomeSearchTarget.Origin) {
            setOrigin(place)
            searchDestination()
            return null
        }
        val trip = tripFor(place) ?: return null
        dismissSearch()
        return trip
    }

    private fun tripFor(destination: TripEndpoint): Pair<TripEndpoint, TripEndpoint>? {
        val origin = _origin.value
        if (origin.isSamePlace(destination)) {
            _localError.value = "Choose a different destination"
            return null
        }
        return origin to destination
    }

    private fun setOrigin(endpoint: TripEndpoint) {
        _origin.value = endpoint
        _localError.value = null
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
            _localError.value = null
            search.clearError()
            try {
                val coordinate = locationProvider.currentCoordinate()
                if (coordinate == null) {
                    _localError.value = "Unable to determine your current location"
                    return@launch
                }
                clearFocusedPlace()
                _followMap.value = true
                applyLocationBias(coordinate, originName = "My location")
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                _localError.value = error.message
            } catch (error: Exception) {
                _localError.value = error.message ?: "Unable to determine your current location"
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun onLocationPermissionDenied(fromUserAction: Boolean = true) {
        if (fromUserAction) {
            _localError.value = "Location permission is required to use your current position"
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
                if (search.query.value.isBlank()) {
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

    companion object {
        private val MAP_CENTER = Coordinate(latitude = 50.8503, longitude = 4.3517)
        private const val NEARBY_RADIUS_METERS = 1_500
        private const val NEARBY_MAX_STOPS = 30
        private const val NEARBY_EVENT_COUNT = 250
        private const val NEARBY_POLL_INTERVAL_MS = 60_000L
    }
}

/** Which field the home search sheet is editing, or null when the sheet is closed. */
enum class HomeSearchTarget { Origin, Destination }
