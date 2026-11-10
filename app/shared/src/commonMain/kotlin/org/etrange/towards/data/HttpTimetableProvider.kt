package org.etrange.towards.data

import io.ktor.client.request.parameter
import org.etrange.towards.api.dto.StopTimesDto
import org.etrange.towards.api.dto.toDomain
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.requests.StopTimesRequest
import org.etrange.towards.domain.port.TimetableProvider

class HttpTimetableProvider(
    private val api: TowardsApi,
) : TimetableProvider {

    override suspend fun getStopTimes(request: StopTimesRequest): StopTimes = mapDomain {
        api.get<StopTimesDto>("/api/v1/stop-times") {
            request.stopId?.let { parameter("stopId", it) }
            request.center?.let { parameter("center", it.toQueryParameter()) }
            request.radiusMeters?.let { parameter("radius", it) }
            request.time?.let { parameter("time", it) }
            if (request.arriveBy) parameter("arriveBy", true)
            parameter("n", request.numberOfEvents)
            request.transportModes.takeIf { it.isNotEmpty() }
                ?.let { parameter("modes", it.joinToString(",") { mode -> mode.name }) }
            request.pageCursor?.let { parameter("pageCursor", it) }
            request.language.takeIf { it.isNotEmpty() }
                ?.let { parameter("language", it.joinToString(",")) }
        }.toDomain()
    }
}
