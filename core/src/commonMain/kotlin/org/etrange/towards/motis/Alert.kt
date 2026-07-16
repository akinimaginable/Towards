package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class TimeRange(
    val start: String,
    val end: String,
)

@Serializable
data class Alert(
    val headerText: String,
    val descriptionText: String,
    val code: String? = null,
    val communicationPeriod: List<TimeRange>? = null,
    val impactPeriod: List<TimeRange>? = null,
    val cause: AlertCause? = null,
    val causeDetail: String? = null,
    val effect: AlertEffect? = null,
    val effectDetail: String? = null,
    val url: String? = null,
    val ttsHeaderText: String? = null,
    val ttsDescriptionText: String? = null,
    val severityLevel: AlertSeverityLevel? = null,
    val imageUrl: String? = null,
    val imageMediaType: String? = null,
    val imageAlternativeText: String? = null,
)
