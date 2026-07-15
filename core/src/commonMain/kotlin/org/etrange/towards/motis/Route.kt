package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class RouteSegment(
    val from: Int,
    val to: Int,
    val polyline: Int,
)

@Serializable
data class RoutePolyline(
    val polyline: EncodedPolyline,
    val colors: List<String>,
    val routeIndexes: List<Int>,
)

@Serializable
data class RouteColor(
    val color: String,
    val textColor: String,
)

@Serializable
data class TransitRouteInfo(
    val id: String,
    val shortName: String,
    val longName: String,
    val color: String? = null,
    val textColor: String? = null,
)

@Serializable
data class RouteInfo(
    val mode: Mode,
    val transitRoutes: List<TransitRouteInfo>,
    val numStops: Int,
    val routeIdx: Int,
    val pathSource: RoutePathSource,
    val segments: List<RouteSegment>,
)

@Serializable
data class RoutesResponse(
    val routes: List<RouteInfo>,
    val polylines: List<RoutePolyline>,
    val stops: List<Place>,
    val zoomFiltered: Boolean,
)
