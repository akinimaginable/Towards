package org.etrange.towards.domain.model.requests

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.LocationKind

data class ReverseGeocodeRequest(
    val coordinate: Coordinate,
    val kinds: Set<LocationKind> = emptySet(),
    val numberOfResults: Int = GeocodeRequest.DEFAULT_RESULT_COUNT,
) {
    init {
        require(numberOfResults in GeocodeRequest.RESULT_COUNT) {
            "numberOfResults must be within ${GeocodeRequest.RESULT_COUNT}"
        }
    }
}
