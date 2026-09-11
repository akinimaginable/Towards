package org.etrange.towards.domain.model.requests

import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.map.MapBounds

data class MapStopsRequest(
    val bounds: MapBounds,
    val grouped: Boolean? = null,
    val modes: Set<TransportMode> = emptySet(),
    val languages: List<String> = emptyList(),
)
