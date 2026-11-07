package org.etrange.towards.data

import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import org.etrange.towards.api.dto.ItineraryDto
import org.etrange.towards.api.dto.RefreshItineraryRequestDto
import org.etrange.towards.api.dto.TripPlanDto
import org.etrange.towards.api.dto.toDomain
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.TripPlan
import org.etrange.towards.domain.model.requests.ItineraryRefreshRequest
import org.etrange.towards.domain.model.requests.TripLookupRequest
import org.etrange.towards.domain.model.requests.TripPlanningRequest
import org.etrange.towards.domain.port.TripInformationProvider
import org.etrange.towards.domain.port.TripPlanner

class HttpTripPlanner(
    private val api: TowardsApi,
) : TripPlanner, TripInformationProvider {

    override suspend fun plan(request: TripPlanningRequest): TripPlan = mapDomain {
        api.get<TripPlanDto>("/api/v1/trips/plan") {
            parameter("from", request.from.toQueryParameter())
            parameter("to", request.to.toQueryParameter())
            request.time?.let { parameter("time", it) }
            if (request.arriveBy) parameter("arriveBy", true)
            request.transitModes.takeIf { it.isNotEmpty() }
                ?.let { parameter("transitModes", it.joinToString(",") { mode -> mode.name }) }
            request.directModes.takeIf { it.isNotEmpty() }
                ?.let { parameter("directModes", it.joinToString(",") { mode -> mode.name }) }
            request.maxTransfers?.let { parameter("maxTransfers", it) }
            request.pageCursor?.let { parameter("pageCursor", it) }
            request.language.takeIf { it.isNotEmpty() }
                ?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()
    }

    override suspend fun getTrip(request: TripLookupRequest): Itinerary = mapDomain {
        api.get<ItineraryDto>("/api/v1/trips/${request.tripId.encodeURLPathPart()}") {
            if (request.includeScheduledSkippedStops) {
                parameter("withScheduledSkippedStops", true)
            }
            request.language.takeIf { it.isNotEmpty() }
                ?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()
    }

    override suspend fun refreshItinerary(request: ItineraryRefreshRequest): Itinerary = mapDomain {
        api.post<ItineraryDto>("/api/v1/itineraries/refresh") {
            contentType(ContentType.Application.Json)
            setBody(
                RefreshItineraryRequestDto(
                    itineraryId = request.itineraryId,
                    language = request.language,
                ),
            )
        }.toDomain()
    }
}
