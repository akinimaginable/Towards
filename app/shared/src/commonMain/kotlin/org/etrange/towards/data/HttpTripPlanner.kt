package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.etrange.towards.api.dto.ErrorResponseDto
import org.etrange.towards.api.dto.TripPlanDto
import org.etrange.towards.api.dto.toDomain
import org.etrange.towards.domain.model.TripPlan
import org.etrange.towards.domain.model.requests.TripPlanningRequest
import org.etrange.towards.domain.port.TripPlanner

class HttpTripPlanner(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) : TripPlanner {

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

    private suspend inline fun <reified T> get(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = try {
            client.get(config.baseUrl.trimEnd('/') + path) {
                accept(ContentType.Application.Json)
                configure()
            }
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
