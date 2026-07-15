package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class StopTimesResponse(
    val stopTimes: List<StopTime>,
    val place: Place,
    val previousPageCursor: String,
    val nextPageCursor: String,
)
