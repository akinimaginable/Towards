package org.etrange.towards.domain.port

import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TripPlan
import org.etrange.towards.domain.model.map.MapBounds
import org.etrange.towards.domain.model.map.MapInitialView
import org.etrange.towards.domain.model.map.MapTrip
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.model.requests.ItineraryRefreshRequest
import org.etrange.towards.domain.model.requests.MapStopsRequest
import org.etrange.towards.domain.model.requests.MapTripsRequest
import org.etrange.towards.domain.model.requests.ReverseGeocodeRequest
import org.etrange.towards.domain.model.requests.StopTimesRequest
import org.etrange.towards.domain.model.requests.TripLookupRequest
import org.etrange.towards.domain.model.requests.TripPlanningRequest

interface TripPlanner {
    suspend fun plan(request: TripPlanningRequest): TripPlan
}

interface TripInformationProvider {
    suspend fun getTrip(request: TripLookupRequest): Itinerary

    suspend fun refreshItinerary(request: ItineraryRefreshRequest): Itinerary
}

interface Geocoder {
    suspend fun geocode(request: GeocodeRequest): List<GeocodeResult>

    suspend fun reverseGeocode(request: ReverseGeocodeRequest): List<GeocodeResult>
}

interface TimetableProvider {
    suspend fun getStopTimes(request: StopTimesRequest): StopTimes
}

interface MapDataProvider {
    suspend fun getInitialMapView(): MapInitialView

    suspend fun getMapStops(request: MapStopsRequest): List<Place>

    suspend fun getMapTrips(request: MapTripsRequest): List<MapTrip>

    suspend fun getMapLevels(bounds: MapBounds): List<Double>
}

interface TransitDataProvider : TimetableProvider, MapDataProvider
