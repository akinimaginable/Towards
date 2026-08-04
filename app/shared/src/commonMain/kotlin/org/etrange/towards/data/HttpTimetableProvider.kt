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
import kotlinx.serialization.json.Json
import org.etrange.towards.api.dto.ErrorResponseDto
import org.etrange.towards.api.dto.StopTimesDto
import org.etrange.towards.api.dto.toDomain
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.StopTimesRequest
import org.etrange.towards.domain.port.TimetableProvider

class HttpTimetableProvider(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) : TimetableProvider {

    override suspend fun getStopTimes(request: StopTimesRequest): StopTimes =
        get<StopTimesDto>("/api/v1/stop-times") {
            request.stopId?.let { parameter("stopId", it) }
            request.center?.let { parameter("center", it.toQueryParameter()) }
            request.radiusMeters?.let { parameter("radius", it) }
            request.time?.let { parameter("time", it) }
            if (request.arriveBy) parameter("arriveBy", true)
            request.numberOfEvents?.let { parameter("n", it) }
            request.transportModes.takeIf { it.isNotEmpty() }
                ?.let { parameter("modes", it.joinToString(",") { mode -> mode.name }) }
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

private fun Coordinate.toQueryParameter(): String = buildString {
    append(latitude)
    append(',')
    append(longitude)
    level?.let {
        append(',')
        append(it)
    }
}
