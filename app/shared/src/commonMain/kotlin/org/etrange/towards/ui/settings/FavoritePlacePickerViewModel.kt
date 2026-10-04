package org.etrange.towards.ui.settings

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
import org.etrange.towards.data.FavoriteLocation
import org.etrange.towards.data.FavoritePlacesStore
import org.etrange.towards.data.FavoriteSaveResult
import org.etrange.towards.data.LocationBiasStore
import org.etrange.towards.data.LocationProvider
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.ui.search.PlaceSearch
import org.etrange.towards.ui.trip.toTripEndpoint

sealed interface FavoritePickerTarget {
    data object Home : FavoritePickerTarget
    data object Work : FavoritePickerTarget
    data class New(val label: String) : FavoritePickerTarget
    data class Replace(val id: String) : FavoritePickerTarget
}

fun favoritePickerTarget(target: String, label: String): FavoritePickerTarget = when (target) {
    FavoritePlacesStore.HOME_ID -> FavoritePickerTarget.Home
    FavoritePlacesStore.WORK_ID -> FavoritePickerTarget.Work
    FavoritePlacesStore.NEW_TARGET -> FavoritePickerTarget.New(label)
    else -> FavoritePickerTarget.Replace(target)
}

class FavoritePlacePickerViewModel(
    private val geocoder: Geocoder,
    private val locationProvider: LocationProvider,
    private val favoritePlacesStore: FavoritePlacesStore,
    private val target: FavoritePickerTarget,
    private val locationBiasStore: LocationBiasStore = LocationBiasStore(),
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

    private var locateJob: Job? = null

    fun onQueryChange(value: String) {
        _localError.value = null
        search.onQueryChange(value)
    }

    fun onSuggestionSelected(result: GeocodeResult): Boolean = commit(result.toFavoriteLocation())

    fun onMyLocationSelected(onResult: (Boolean) -> Unit) {
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            _isLocating.value = true
            _localError.value = null
            search.clearError()
            try {
                val coordinate = locationProvider.currentCoordinate()
                    ?: locationProvider.lastKnownCoordinate()
                    ?: locationBiasStore.load()
                if (coordinate == null) {
                    _localError.value = "Unable to determine your current location"
                    onResult(false)
                    return@launch
                }
                onResult(commit(FavoriteLocation(name = "My location", coordinate = coordinate)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _localError.value = error.message ?: "Unable to determine your current location"
                onResult(false)
            } finally {
                _isLocating.value = false
            }
        }
    }

    private fun commit(location: FavoriteLocation): Boolean {
        val result = when (val target = target) {
            FavoritePickerTarget.Home -> {
                favoritePlacesStore.setHome(location)
                FavoriteSaveResult.Success
            }
            FavoritePickerTarget.Work -> {
                favoritePlacesStore.setWork(location)
                FavoriteSaveResult.Success
            }
            is FavoritePickerTarget.New -> favoritePlacesStore.add(target.label, location)
            is FavoritePickerTarget.Replace -> favoritePlacesStore.replace(target.id, location)
        }
        return when (result) {
            FavoriteSaveResult.Success -> true
            is FavoriteSaveResult.Failure -> {
                _localError.value = result.message
                false
            }
        }
    }
}

private fun GeocodeResult.toFavoriteLocation() = FavoriteLocation(
    name = name.ifBlank { "Saved place" },
    coordinate = coordinate,
    stopId = toTripEndpoint().stopId,
)
