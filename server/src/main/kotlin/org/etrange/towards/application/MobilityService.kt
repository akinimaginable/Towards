package org.etrange.towards.application

import org.etrange.towards.domain.model.ActorContext
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.LocationReference
import kotlin.time.Clock
import kotlin.time.Instant
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TripPlan
import org.etrange.towards.domain.model.nearby.NearbyDeparturesLimits
import org.etrange.towards.domain.model.nearby.NearbyStop
import org.etrange.towards.domain.model.nearby.groupNearbyDepartures
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.model.requests.ItineraryRefreshRequest
import org.etrange.towards.domain.model.requests.ReverseGeocodeRequest
import org.etrange.towards.domain.model.requests.StopTimesRequest
import org.etrange.towards.domain.model.requests.TripLookupRequest
import org.etrange.towards.domain.model.requests.TripPlanningRequest
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.domain.port.TimetableProvider
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
    private val timetableProvider: TimetableProvider,
    private val auditService: AuditService,
    private val tripPlanCache: TripPlanCache,
    private val nearbyStopTimesCache: NearbyStopTimesCache,
) {
    suspend fun plan(
        actor: ActorContext?,
        correlationId: String,
        request: TripPlanningRequest,
    ): TripPlan = audited(
        actor = actor,
        correlationId = correlationId,
        action = AuditAction.TRIP_SEARCH,
        requestSummary = request.toAuditSummary(),
        resultSummary = { "itineraries=${it.itineraries.size},direct=${it.direct.size}" },
    ) {
        tripPlanCache.get(request) ?: tripPlanner.plan(request).also {
            tripPlanCache.put(request, it)
        }
    }

    suspend fun getTrip(
        actor: ActorContext?,
        correlationId: String,
        request: TripLookupRequest,
    ): Itinerary = audited(
        actor,
        correlationId,
        AuditAction.TRIP_LOOKUP,
        "tripIdPresent=${request.tripId.isNotBlank()},languageCount=${request.language.size}",
        { "legs=${it.legs.size}" },
    ) {
        tripInformationProvider.getTrip(request)
    }

    suspend fun refreshItinerary(
        actor: ActorContext?,
        correlationId: String,
        request: ItineraryRefreshRequest,
    ): Itinerary = audited(
        actor,
        correlationId,
        AuditAction.ITINERARY_REFRESH,
        "itineraryIdPresent=${request.itineraryId.isNotBlank()},languageCount=${request.language.size}",
        { "legs=${it.legs.size}" },
    ) {
        tripInformationProvider.refreshItinerary(request)
    }

    suspend fun getStopTimes(
        actor: ActorContext?,
        correlationId: String,
        request: StopTimesRequest,
    ): StopTimes = audited(
        actor,
        correlationId,
        AuditAction.STOP_TIMES,
        request.toAuditSummary(),
        { "events=${it.events.size}" },
    ) {
        timetableProvider.getStopTimes(request)
    }

    suspend fun nearbyDepartures(
        actor: ActorContext?,
        correlationId: String,
        center: Coordinate,
    ): List<NearbyStop> = audited(
        actor,
        correlationId,
        AuditAction.NEARBY,
        "radius=${NearbyDeparturesLimits.RADIUS_METERS}",
        { "stops=${it.size}" },
    ) {
        val now = Clock.System.now()
        val minute = now.epochSeconds / 60
        val cell = center.nearbyCell()
        val stopTimes = nearbyStopTimesCache.getOrLoad(cell, minute) {
            timetableProvider.getStopTimes(
                StopTimesRequest(
                    center = cell.center(),
                    radiusMeters = NearbyDeparturesLimits.RADIUS_METERS,
                    numberOfEvents = NearbyDeparturesLimits.UPSTREAM_EVENT_COUNT,
                    time = now.toSecondPrecision(),
                ),
            )
        }
        groupNearbyDepartures(
            stopTimes = stopTimes,
            origin = center,
            maxStops = NearbyDeparturesLimits.MAX_STOPS,
            now = now,
            laterTimesPerLine = NearbyDeparturesLimits.LATER_TIMES_PER_LINE,
        )
    }

    suspend fun geocode(
        actor: ActorContext?,
        correlationId: String,
        request: GeocodeRequest,
    ): List<GeocodeResult> = audited(
        actor,
        correlationId,
        AuditAction.GEOCODE,
        request.toAuditSummary(),
        { "results=${it.size}" },
    ) {
        geocoder.geocode(request)
    }

    suspend fun reverseGeocode(
        actor: ActorContext?,
        correlationId: String,
        request: ReverseGeocodeRequest,
    ): List<GeocodeResult> = audited(
        actor,
        correlationId,
        AuditAction.REVERSE_GEOCODE,
        request.toAuditSummary(),
        { "results=${it.size}" },
    ) {
        geocoder.reverseGeocode(request)
    }

    private suspend fun <T> audited(
        actor: ActorContext?,
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
                    errorDetails = cause::class.simpleName,
                    durationMillis = elapsedMillis(startedAt),
                ),
            )
            throw cause
        }
    }

    private fun elapsedMillis(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000
}

private fun Instant.toSecondPrecision(): String = Instant.fromEpochSeconds(epochSeconds).toString()

private fun LocationReference.auditKind(): String = when (this) {
    is LocationReference.Stop -> "stop"
    is LocationReference.Position -> "position"
}

internal fun TripPlanningRequest.toAuditSummary(): String =
    "from=${from.auditKind()},to=${to.auditKind()},arriveBy=$arriveBy," +
        "transitModes=${transitModes.size},directModes=${directModes.size}," +
        "hasTime=${time != null},maxTransfersSet=${maxTransfers != null}," +
        "hasPageCursor=${pageCursor != null},languageCount=${language.size}"

internal fun StopTimesRequest.toAuditSummary(): String =
    "source=${if (stopId != null) "stop" else "area"},radiusSet=${radiusMeters != null}," +
        "events=$numberOfEvents,arriveBy=$arriveBy,modes=${transportModes.size}," +
        "hasTime=${time != null},hasPageCursor=${pageCursor != null},languageCount=${language.size}"

internal fun GeocodeRequest.toAuditSummary(): String =
    "queryLength=${text.length},hasBias=${bias != null},kinds=${kinds.size}," +
        "modes=${modes.size},limit=$numberOfResults,languageCount=${languages.size}"

internal fun ReverseGeocodeRequest.toAuditSummary(): String =
    "kinds=${kinds.size},limit=$numberOfResults"
