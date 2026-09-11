package org.etrange.towards.domain.model.requests

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.TransportMode

data class StopTimesRequest(
    val stopId: String? = null,
    val center: Coordinate? = null,
    val radiusMeters: Int? = null,
    val time: String? = null,
    val arriveBy: Boolean = false,
    val numberOfEvents: Int? = null,
    val transportModes: Set<TransportMode> = setOf(TransportMode.TRANSIT),
    val pageCursor: String? = null,
    val language: List<String> = emptyList(),
)
