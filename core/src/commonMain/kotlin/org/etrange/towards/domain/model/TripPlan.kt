package org.etrange.towards.domain.model

data class TripPlan(
    val from: Place,
    val to: Place,
    val direct: List<Itinerary>,
    val itineraries: List<Itinerary>,
    val previousPageCursor: String?,
    val nextPageCursor: String?,
)
