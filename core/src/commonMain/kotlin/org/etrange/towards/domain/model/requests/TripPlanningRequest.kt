package org.etrange.towards.domain.model.requests

import org.etrange.towards.domain.model.LocationReference
import org.etrange.towards.domain.model.TransportMode

data class TripPlanningRequest(
    val from: LocationReference,
    val to: LocationReference,
    val time: String? = null,
    val arriveBy: Boolean = false,
    val transitModes: Set<TransportMode> = setOf(TransportMode.TRANSIT),
    val directModes: Set<TransportMode> = setOf(TransportMode.WALK),
    val maxTransfers: Int? = null,
    val pageCursor: String? = null,
    val language: List<String> = emptyList(),
)
