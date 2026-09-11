package org.etrange.towards.domain.model.requests

import org.etrange.towards.domain.model.map.MapBounds

data class MapTripsRequest(
    val bounds: MapBounds,
    val zoom: Double,
    val startTime: String,
    val endTime: String,
    val precision: Int = 5,
    val languages: List<String> = emptyList(),
)
