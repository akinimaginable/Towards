package org.etrange.towards.data

import io.ktor.client.request.parameter
import org.etrange.towards.api.dto.NearbyStopDto
import org.etrange.towards.api.dto.toDomain
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.nearby.NearbyStop
import org.etrange.towards.domain.port.NearbyDeparturesProvider

class HttpNearbyDepartures(
    private val api: TowardsApi,
) : NearbyDeparturesProvider {

    override suspend fun nearby(center: Coordinate): List<NearbyStop> = mapDomain {
        val stops = api.get<List<NearbyStopDto>>("/api/v1/nearby") {
            parameter("center", center.toQueryParameter())
        }
        val decoded = stops.mapNotNull { it.toDomain() }
        if (stops.isNotEmpty() && decoded.isEmpty()) {
            throw ApiException(ApiError.MalformedResponse)
        }
        decoded
    }
}
