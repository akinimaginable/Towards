package org.etrange.towards.domain.port

import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.ItineraryRefreshRequest
import org.etrange.towards.domain.model.MapBounds
import org.etrange.towards.domain.model.MapInitialView
import org.etrange.towards.domain.model.MapStopsRequest
import org.etrange.towards.domain.model.MapTrip
import org.etrange.towards.domain.model.MapTripsRequest
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.ReverseGeocodeRequest
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.StopTimesRequest
import org.etrange.towards.domain.model.TripLookupRequest
import org.etrange.towards.domain.model.TripPlan
import org.etrange.towards.domain.model.TripPlanningRequest

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

interface TransitDataProvider {
    suspend fun getStopTimes(request: StopTimesRequest): StopTimes

    suspend fun getInitialMapView(): MapInitialView

    suspend fun getMapStops(request: MapStopsRequest): List<Place>

    suspend fun getMapTrips(request: MapTripsRequest): List<MapTrip>

    suspend fun getMapLevels(bounds: MapBounds): List<Double>
}
