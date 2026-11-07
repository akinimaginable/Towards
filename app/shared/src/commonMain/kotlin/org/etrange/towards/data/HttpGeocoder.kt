package org.etrange.towards.data

import io.ktor.client.request.parameter
import org.etrange.towards.api.dto.GeocodeResultDto
import org.etrange.towards.api.dto.toGeocodeResults
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.model.requests.ReverseGeocodeRequest
import org.etrange.towards.domain.port.Geocoder

class HttpGeocoder(
    private val api: TowardsApi,
) : Geocoder {

    override suspend fun geocode(request: GeocodeRequest): List<GeocodeResult> = mapDomain {
        api.get<List<GeocodeResultDto>>("/api/v1/geocode") {
            parameter("text", request.text)
            request.languages.takeIf { it.isNotEmpty() }
                ?.let { parameter("language", it.joinToString(",")) }
            request.kinds.takeIf { it.isNotEmpty() }
                ?.let { parameter("types", it.joinToString(",") { kind -> kind.name }) }
            request.modes.takeIf { it.isNotEmpty() }
                ?.let { parameter("modes", it.joinToString(",") { mode -> mode.name }) }
            request.bias?.let { parameter("bias", it.toQueryParameter()) }
            request.numberOfResults?.let { parameter("limit", it) }
        }.toDecodedResults()
    }

    override suspend fun reverseGeocode(request: ReverseGeocodeRequest): List<GeocodeResult> = mapDomain {
        api.get<List<GeocodeResultDto>>("/api/v1/reverse-geocode") {
            parameter("place", request.coordinate.toQueryParameter())
            request.kinds.takeIf { it.isNotEmpty() }
                ?.let { parameter("types", it.joinToString(",") { kind -> kind.name }) }
            request.numberOfResults?.let { parameter("limit", it) }
        }.toDecodedResults()
    }
}

private fun List<GeocodeResultDto>.toDecodedResults(): List<GeocodeResult> {
    val decoded = toGeocodeResults()
    if (isNotEmpty() && decoded.isEmpty()) {
        throw ApiException(ApiError.MalformedResponse)
    }
    return decoded
}
