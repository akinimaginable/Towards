package org.etrange.towards.domain.model.requests

data class ItineraryRefreshRequest(
    val itineraryId: String,
    val language: List<String> = emptyList(),
)
