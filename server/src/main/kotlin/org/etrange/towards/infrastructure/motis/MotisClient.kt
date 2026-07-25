package org.etrange.towards.infrastructure.motis

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.etrange.towards.application.BadRequestException
import org.etrange.towards.application.NotFoundException
import org.etrange.towards.application.UpstreamServiceException
import org.etrange.towards.config.MotisConfig
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.ItineraryRefreshRequest
import org.etrange.towards.domain.model.LocationReference
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

class MotisClient(
    private val client: HttpClient,
    private val config: MotisConfig,
    private val json: Json,
) : TripPlanner, TripInformationProvider, Geocoder, TransitDataProvider {

    override suspend fun plan(request: TripPlanningRequest): TripPlan =
        get<MotisPlanResponse>("/api/v6/plan") {
            parameter("fromPlace", request.from.toMotisParameter())
            parameter("toPlace", request.to.toMotisParameter())
            request.time?.let { parameter("time", it) }
            parameter("arriveBy", request.arriveBy)
            parameter("transitModes", request.transitModes.joinToString(",") { it.name })
            parameter("directModes", request.directModes.joinToString(",") { it.name })
            request.maxTransfers?.let { parameter("maxTransfers", it) }
            request.pageCursor?.let { parameter("pageCursor", it) }
            request.language.takeIf { it.isNotEmpty() }?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()

    override suspend fun getTrip(request: TripLookupRequest): Itinerary =
        get<MotisItinerary>("/api/v6/trip") {
            parameter("tripId", request.tripId)
            parameter("withScheduledSkippedStops", request.includeScheduledSkippedStops)
            request.language.takeIf { it.isNotEmpty() }?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()
            ?: throw NotFoundException("Trip is unavailable or uses unsupported transport modes")

    override suspend fun refreshItinerary(request: ItineraryRefreshRequest): Itinerary =
        get<MotisItinerary>("/api/v6/refresh-itinerary") {
            parameter("itineraryId", request.itineraryId)
            request.language.takeIf { it.isNotEmpty() }?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()
            ?: throw NotFoundException("Itinerary is unavailable or uses unsupported transport modes")

    override suspend fun getStopTimes(request: StopTimesRequest): StopTimes {
        if (request.stopId == null && request.center == null) {
            throw BadRequestException("Either stopId or center must be provided")
        }
        if (request.stopId == null && request.radiusMeters == null) {
            throw BadRequestException("radius is required when stop times are queried by center")
        }

        return get<MotisStopTimesResponse>("/api/v6/stoptimes") {
            request.stopId?.let { parameter("stopId", it) }
            request.center?.let { parameter("center", it.toMotisParameter()) }
            request.radiusMeters?.let { parameter("radius", it) }
            request.time?.let { parameter("time", it) }
            parameter("arriveBy", request.arriveBy)
            request.numberOfEvents?.let { parameter("n", it) }
            parameter("mode", request.transportModes.joinToString(",") { it.name })
            request.pageCursor?.let { parameter("pageCursor", it) }
            request.language.takeIf { it.isNotEmpty() }?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()
    }

    override suspend fun geocode(request: GeocodeRequest): List<GeocodeResult> =
        get<List<MotisMatch>>("/api/v1/geocode") {
            parameter("text", request.text)
            request.languages.takeIf { it.isNotEmpty() }?.let { parameter("language", it.joinToString(",")) }
            request.kinds.takeIf { it.isNotEmpty() }?.let { parameter("type", it.joinToString(",") { kind -> kind.name }) }
            request.modes.takeIf { it.isNotEmpty() }?.let { parameter("mode", it.joinToString(",") { mode -> mode.name }) }
            request.bias?.let { parameter("place", it.toMotisParameter()) }
            request.numberOfResults?.let { parameter("numResults", it) }
        }.map { it.toDomain() }

    override suspend fun reverseGeocode(request: ReverseGeocodeRequest): List<GeocodeResult> =
        get<List<MotisMatch>>("/api/v1/reverse-geocode") {
            parameter("place", request.coordinate.toMotisParameter())
            request.kinds.takeIf { it.isNotEmpty() }?.let { parameter("type", it.joinToString(",") { kind -> kind.name }) }
            request.numberOfResults?.let { parameter("numResults", it) }
        }.map { it.toDomain() }

    override suspend fun getInitialMapView(): MapInitialView =
        get<MotisInitialResponse>("/api/v1/map/initial").toDomain()

    override suspend fun getMapStops(request: MapStopsRequest): List<Place> =
        get<List<MotisPlace>>("/api/v6/map/stops") {
            parameter("min", request.bounds.minimum.toMotisParameter())
            parameter("max", request.bounds.maximum.toMotisParameter())
            request.grouped?.let { parameter("grouped", it) }
            request.modes.takeIf { it.isNotEmpty() }?.let { parameter("modes", it.joinToString(",") { mode -> mode.name }) }
            request.languages.takeIf { it.isNotEmpty() }?.let { parameter("language", it.joinToString(",")) }
        }.map { it.toDomain() }

    override suspend fun getMapTrips(request: MapTripsRequest): List<MapTrip> =
        get<List<MotisTripSegment>>("/api/v6/map/trips") {
            parameter("zoom", request.zoom)
            parameter("min", request.bounds.minimum.toMotisParameter())
            parameter("max", request.bounds.maximum.toMotisParameter())
            parameter("startTime", request.startTime)
            parameter("endTime", request.endTime)
            parameter("precision", request.precision)
            request.languages.takeIf { it.isNotEmpty() }?.let { parameter("language", it.joinToString(",")) }
        }.mapNotNull { it.toDomain() }

    override suspend fun getMapLevels(bounds: MapBounds): List<Double> =
        get("/api/v1/map/levels") {
            parameter("min", bounds.minimum.toMotisParameter())
            parameter("max", bounds.maximum.toMotisParameter())
        }

    private suspend inline fun <reified T> get(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = try {
            client.get(config.baseUrl + path) {
                accept(ContentType.Application.Json)
                configure()
            }
        } catch (cause: Exception) {
            throw UpstreamServiceException(
                HttpStatusCode.ServiceUnavailable,
                "The trip planning service is unavailable",
                cause,
            )
        }

        if (response.status.isSuccess()) {
            return response.body()
        }

        val body = response.bodyAsText()
        val message = runCatching { json.decodeFromString<MotisError>(body).error }
            .getOrDefault("The trip planning service rejected the request")

        throw when (response.status) {
            HttpStatusCode.NotFound -> NotFoundException(message)
            HttpStatusCode.BadRequest, HttpStatusCode.UnprocessableEntity -> BadRequestException(message)
            else -> UpstreamServiceException(HttpStatusCode.BadGateway, message)
        }
    }
}

private fun LocationReference.toMotisParameter(): String = when (this) {
    is LocationReference.Stop -> id
    is LocationReference.Position -> coordinate.toMotisParameter()
}

private fun Coordinate.toMotisParameter(): String = buildString {
    append(latitude)
    append(',')
    append(longitude)
    level?.let {
        append(',')
        append(it)
    }
}
