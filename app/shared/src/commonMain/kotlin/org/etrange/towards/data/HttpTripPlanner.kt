package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.etrange.towards.api.dto.ErrorResponseDto
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
    private val client: HttpClient,
    private val config: ApiConfig,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) : TripPlanner, TripInformationProvider {

    override suspend fun plan(request: TripPlanningRequest): TripPlan =
        get<TripPlanDto>("/api/v1/trips/plan") {
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

    override suspend fun getTrip(request: TripLookupRequest): Itinerary =
        get<ItineraryDto>("/api/v1/trips/${request.tripId.encodeURLPathPart()}") {
            if (request.includeScheduledSkippedStops) {
                parameter("withScheduledSkippedStops", true)
            }
            request.language.takeIf { it.isNotEmpty() }
                ?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()

    override suspend fun refreshItinerary(request: ItineraryRefreshRequest): Itinerary =
        post<ItineraryDto>("/api/v1/itineraries/refresh") {
            contentType(ContentType.Application.Json)
            setBody(
                RefreshItineraryRequestDto(
                    itineraryId = request.itineraryId,
                    language = request.language,
                ),
            )
        }.toDomain()

    private suspend inline fun <reified T> get(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T = request {
        client.get(config.baseUrl.trimEnd('/') + path) {
            accept(ContentType.Application.Json)
            configure()
        }
    }

    private suspend inline fun <reified T> post(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T = request {
        client.post(config.baseUrl.trimEnd('/') + path) {
            accept(ContentType.Application.Json)
            configure()
        }
    }

    private suspend inline fun <reified T> request(
        call: () -> HttpResponse,
    ): T {
        val response = try {
            call()
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            throw ApiException(
                statusCode = 0,
                message = "Unable to reach the Towards API",
                cause = cause,
            )
        }

        if (response.status.isSuccess()) {
            return response.body()
        }

        val body = response.bodyAsText()
        val error = runCatching { json.decodeFromString<ErrorResponseDto>(body) }.getOrNull()
        throw ApiException(
            statusCode = response.status.value,
            message = error?.message ?: "The Towards API rejected the request",
            correlationId = error?.correlationId,
        )
    }
}
