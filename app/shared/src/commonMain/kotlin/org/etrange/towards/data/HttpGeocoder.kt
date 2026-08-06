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
import org.etrange.towards.api.dto.GeocodeResultDto
import org.etrange.towards.api.dto.toDomain
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.ReverseGeocodeRequest
import org.etrange.towards.domain.model.ensureUniqueIds
import org.etrange.towards.domain.port.Geocoder

class HttpGeocoder(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) : Geocoder {

    override suspend fun geocode(request: GeocodeRequest): List<GeocodeResult> =
        get<List<GeocodeResultDto>>("/api/v1/geocode") {
            parameter("text", request.text)
            request.languages.takeIf { it.isNotEmpty() }
                ?.let { parameter("language", it.joinToString(",")) }
            request.kinds.takeIf { it.isNotEmpty() }
                ?.let { parameter("types", it.joinToString(",") { kind -> kind.name }) }
            request.modes.takeIf { it.isNotEmpty() }
                ?.let { parameter("modes", it.joinToString(",") { mode -> mode.name }) }
            request.bias?.let { parameter("bias", it.toQueryParameter()) }
            request.numberOfResults?.let { parameter("limit", it) }
        }.map { it.toDomain() }.ensureUniqueIds()

    override suspend fun reverseGeocode(request: ReverseGeocodeRequest): List<GeocodeResult> =
        get<List<GeocodeResultDto>>("/api/v1/reverse-geocode") {
            parameter("place", request.coordinate.toQueryParameter())
            request.kinds.takeIf { it.isNotEmpty() }
                ?.let { parameter("types", it.joinToString(",") { kind -> kind.name }) }
            request.numberOfResults?.let { parameter("limit", it) }
        }.map { it.toDomain() }.ensureUniqueIds()

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
