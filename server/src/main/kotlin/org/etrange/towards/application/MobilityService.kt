package org.etrange.towards.application

import org.etrange.towards.domain.model.ActorContext
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
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.domain.port.TransitDataProvider
import org.etrange.towards.domain.port.TripInformationProvider
import org.etrange.towards.domain.port.TripPlanner

interface TripPlanCache {
    suspend fun get(request: TripPlanningRequest): TripPlan?

    suspend fun put(request: TripPlanningRequest, result: TripPlan)
}

class PassThroughTripPlanCache : TripPlanCache {
    override suspend fun get(request: TripPlanningRequest): TripPlan? = null

    override suspend fun put(request: TripPlanningRequest, result: TripPlan) = Unit
}

class MobilityService(
    private val tripPlanner: TripPlanner,
    private val tripInformationProvider: TripInformationProvider,
    private val geocoder: Geocoder,
    private val transitDataProvider: TransitDataProvider,
    private val auditService: AuditService,
    private val tripPlanCache: TripPlanCache,
) {
    suspend fun plan(
        actor: ActorContext,
        correlationId: String,
        request: TripPlanningRequest,
    ): TripPlan = audited(
        actor = actor,
        correlationId = correlationId,
        action = AuditAction.TRIP_SEARCH,
        requestSummary = request.toString(),
        resultSummary = { "itineraries=${it.itineraries.size},direct=${it.direct.size}" },
    ) {
        tripPlanCache.get(request) ?: tripPlanner.plan(request).also {
            tripPlanCache.put(request, it)
        }
    }

    suspend fun getTrip(
        actor: ActorContext,
        correlationId: String,
        request: TripLookupRequest,
    ): Itinerary = audited(
        actor,
        correlationId,
        AuditAction.TRIP_LOOKUP,
        "tripId=${request.tripId}",
        { "legs=${it.legs.size}" },
    ) {
        tripInformationProvider.getTrip(request)
    }

    suspend fun refreshItinerary(
        actor: ActorContext,
        correlationId: String,
        request: ItineraryRefreshRequest,
    ): Itinerary = audited(
        actor,
        correlationId,
        AuditAction.ITINERARY_REFRESH,
        "itineraryId=${request.itineraryId.take(128)}",
        { "legs=${it.legs.size}" },
    ) {
        tripInformationProvider.refreshItinerary(request)
    }

    suspend fun getStopTimes(
        actor: ActorContext,
        correlationId: String,
        request: StopTimesRequest,
    ): StopTimes = audited(
        actor,
        correlationId,
        AuditAction.STOP_TIMES,
        request.toString(),
        { "events=${it.events.size}" },
    ) {
        transitDataProvider.getStopTimes(request)
    }

    suspend fun geocode(
        actor: ActorContext,
        correlationId: String,
        request: GeocodeRequest,
    ): List<GeocodeResult> = audited(
        actor,
        correlationId,
        AuditAction.GEOCODE,
        request.toString(),
        { "results=${it.size}" },
    ) {
        geocoder.geocode(request)
    }

    suspend fun reverseGeocode(
        actor: ActorContext,
        correlationId: String,
        request: ReverseGeocodeRequest,
    ): List<GeocodeResult> = audited(
        actor,
        correlationId,
        AuditAction.REVERSE_GEOCODE,
        request.toString(),
        { "results=${it.size}" },
    ) {
        geocoder.reverseGeocode(request)
    }

    suspend fun getInitialMapView(
        actor: ActorContext,
        correlationId: String,
    ): MapInitialView = audited(
        actor,
        correlationId,
        AuditAction.MAP_INITIAL,
        null,
        { "zoom=${it.zoom}" },
    ) {
        transitDataProvider.getInitialMapView()
    }

    suspend fun getMapStops(
        actor: ActorContext,
        correlationId: String,
        request: MapStopsRequest,
    ): List<Place> = audited(
        actor,
        correlationId,
        AuditAction.MAP_STOPS,
        request.toString(),
        { "stops=${it.size}" },
    ) {
        transitDataProvider.getMapStops(request)
    }

    suspend fun getMapTrips(
        actor: ActorContext,
        correlationId: String,
        request: MapTripsRequest,
    ): List<MapTrip> = audited(
        actor,
        correlationId,
        AuditAction.MAP_TRIPS,
        request.toString(),
        { "trips=${it.size}" },
    ) {
        transitDataProvider.getMapTrips(request)
    }

    suspend fun getMapLevels(
        actor: ActorContext,
        correlationId: String,
        bounds: MapBounds,
    ): List<Double> = audited(
        actor,
        correlationId,
        AuditAction.MAP_LEVELS,
        bounds.toString(),
        { "levels=${it.size}" },
    ) {
        transitDataProvider.getMapLevels(bounds)
    }

    private suspend fun <T> audited(
        actor: ActorContext,
        correlationId: String,
        action: AuditAction,
        requestSummary: String?,
        resultSummary: (T) -> String,
        block: suspend () -> T,
    ): T {
        val startedAt = System.nanoTime()
        return try {
            val result = block()
            auditService.record(
                AuditRecord(
                    actor = actor,
                    correlationId = correlationId,
                    action = action,
                    outcome = AuditOutcome.SUCCESS,
                    requestSummary = requestSummary,
                    resultSummary = resultSummary(result),
                    errorDetails = null,
                    durationMillis = elapsedMillis(startedAt),
                ),
            )
            result
        } catch (cause: Throwable) {
            auditService.record(
                AuditRecord(
                    actor = actor,
                    correlationId = correlationId,
                    action = action,
                    outcome = AuditOutcome.FAILURE,
                    requestSummary = requestSummary,
                    resultSummary = null,
                    errorDetails = "${cause::class.simpleName}: ${cause.message}".take(2_000),
                    durationMillis = elapsedMillis(startedAt),
                ),
            )
            throw cause
        }
    }

    private fun elapsedMillis(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000
}
