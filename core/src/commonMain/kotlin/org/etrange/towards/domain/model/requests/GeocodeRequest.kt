package org.etrange.towards.domain.model.requests

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.TransportMode

data class GeocodeRequest(
    val text: String,
    val languages: List<String> = emptyList(),
    val kinds: Set<LocationKind> = emptySet(),
    val modes: Set<TransportMode> = emptySet(),
    val bias: Coordinate? = null,
    val numberOfResults: Int? = null,
)
