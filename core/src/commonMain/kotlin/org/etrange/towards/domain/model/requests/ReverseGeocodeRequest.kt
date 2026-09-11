package org.etrange.towards.domain.model.requests

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.LocationKind

data class ReverseGeocodeRequest(
    val coordinate: Coordinate,
    val kinds: Set<LocationKind> = emptySet(),
    val numberOfResults: Int? = null,
)
