package org.etrange.towards.domain.port

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TripPlan
import org.etrange.towards.domain.model.nearby.NearbyStop
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.model.requests.ItineraryRefreshRequest
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

interface NearbyDeparturesProvider {
    suspend fun nearby(center: Coordinate): List<NearbyStop>
}
