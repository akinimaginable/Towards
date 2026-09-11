package org.etrange.towards.domain.model

data class Itinerary(
    val id: String,
    val durationSeconds: Int,
    val startTime: String,
    val endTime: String,
    val transfers: Int,
    val legs: List<JourneyLeg>,
)
