package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class PlanResponse(
    val requestParameters: Map<String, String>,
    val debugOutput: Map<String, Int>,
    val from: Place,
    val to: Place,
    val direct: List<Itinerary>,
    val itineraries: List<Itinerary>,
    val previousPageCursor: String,
    val nextPageCursor: String,
)
