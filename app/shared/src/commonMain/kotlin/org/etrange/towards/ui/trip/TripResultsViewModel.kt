package org.etrange.towards.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.etrange.towards.data.ApiException
import org.etrange.towards.data.toApiDateTime
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.requests.TripPlanningRequest
import org.etrange.towards.domain.port.TripPlanner
import kotlin.time.Clock

class TripResultsViewModel(
    private val tripPlanner: TripPlanner,
    origin: TripEndpoint,
    destination: TripEndpoint,
    /** The device's last known position, shown as the "you are here" marker; null when unknown. */
    val userLocation: Coordinate? = null,
) : ViewModel() {
    private val _origin = MutableStateFlow(origin)
    val origin: StateFlow<TripEndpoint> = _origin.asStateFlow()

    private val _destination = MutableStateFlow(destination)
    val destination: StateFlow<TripEndpoint> = _destination.asStateFlow()

    private val _itineraries = MutableStateFlow<List<Itinerary>>(emptyList())
    val itineraries: StateFlow<List<Itinerary>> = _itineraries.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var planJob: Job? = null

    init {
        refresh()
    }

    fun onItineraryClick(id: String) {
        _selectedId.value = id
    }

    fun swapEndpoints() {
        val previousOrigin = _origin.value
        _origin.value = _destination.value
        _destination.value = previousOrigin
        refresh()
    }

    fun refresh() {
        planJob?.cancel()
        planJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val plan = tripPlanner.plan(
                    TripPlanningRequest(
                        from = _origin.value.toLocationReference(),
                        to = _destination.value.toLocationReference(),
                        time = Clock.System.now().toApiDateTime(),
                    ),
                )
                val options = combineTripOptions(plan)
                _itineraries.value = options
                _selectedId.value = options.firstOrNull()?.id
                if (options.isEmpty()) {
                    _errorMessage.value = "No trips found between these places"
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                _itineraries.value = emptyList()
                _selectedId.value = null
                _errorMessage.value = error.message
            } catch (error: Exception) {
                _itineraries.value = emptyList()
                _selectedId.value = null
                _errorMessage.value = error.message ?: "Unable to plan this trip"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
