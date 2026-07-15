package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val name: String,
    val lat: Double,
    val lon: Double,
    val stopId: String? = null,
    val parentId: String? = null,
    val importance: Double? = null,
    val level: Double? = null,
    val tz: String? = null,
    val arrival: String? = null,
    val departure: String? = null,
    val scheduledArrival: String? = null,
    val scheduledDeparture: String? = null,
    val scheduledTrack: String? = null,
    val track: String? = null,
    val stopCode: String? = null,
    val description: String? = null,
    val vertexType: VertexType? = null,
    val pickupType: PickupDropoffType? = null,
    val dropoffType: PickupDropoffType? = null,
    val cancelled: Boolean? = null,
    val alerts: List<Alert>? = null,
    val flex: String? = null,
    val flexId: String? = null,
    val flexStartPickupDropOffWindow: String? = null,
    val flexEndPickupDropOffWindow: String? = null,
    val modes: List<Mode>? = null,
)
