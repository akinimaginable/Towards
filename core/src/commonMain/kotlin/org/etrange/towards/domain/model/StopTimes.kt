package org.etrange.towards.domain.model

data class StopTimes(
    val place: Place,
    val events: List<StopTime>,
    val previousPageCursor: String?,
    val nextPageCursor: String?,
)
