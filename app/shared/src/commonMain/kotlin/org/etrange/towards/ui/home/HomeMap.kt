package org.etrange.towards.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import org.etrange.towards.data.decodeCoordinates
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.ui.trip.isStreetMode
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import towards.app.shared.generated.resources.Res
import kotlin.time.Duration.Companion.milliseconds

private val DefaultMapCenter = Coordinate(latitude = 50.8503, longitude = 4.3517)
private const val DefaultZoom = 15.0

@Composable
fun HomeMap(
    center: Coordinate?,
    modifier: Modifier = Modifier,
    userLocation: Coordinate? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    routeLegs: List<JourneyLeg> = emptyList(),
    origin: Coordinate? = null,
    destination: Coordinate? = null,
    fitRoute: Boolean = false,
    followCenter: Boolean = true,
    onUserMovedCamera: () -> Unit = {},
    onMapCameraIdle: (Coordinate) -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Map preview",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    val target = center ?: DefaultMapCenter
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = target.longitude, latitude = target.latitude),
            zoom = DefaultZoom,
        ),
    )
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val styleUri = if (darkTheme) {
        Res.getUri("files/towards_map_dark.json")
    } else {
        Res.getUri("files/towards_map_light.json")
    }
    var previousCenter by remember { mutableStateOf(center) }
    var previousFollowCenter by remember { mutableStateOf(followCenter) }
    val cameraTopPadding = contentPadding.calculateTopPadding()
    val cameraBottomPadding = contentPadding.calculateBottomPadding()
    val routeSignature = routeLegs.joinToString("|") { leg ->
        "${leg.startTime}:${leg.to.name}:${leg.geometry?.points.orEmpty().take(24)}"
    }

    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.moveReason to cameraState.position.target }
            .filter { (reason, _) -> reason == CameraMoveReason.GESTURE }
            .map { (_, targetPosition) ->
                Coordinate(
                    latitude = targetPosition.latitude,
                    longitude = targetPosition.longitude,
                )
            }
            .distinctUntilChanged()
            .collect { focus ->
                onUserMovedCamera()
                onMapCameraIdle(focus)
            }
    }

    LaunchedEffect(
        target.latitude,
        target.longitude,
        cameraTopPadding,
        cameraBottomPadding,
        routeSignature,
        fitRoute,
        followCenter,
    ) {
        cameraState.awaitViewport()
        val bounds = if (fitRoute) routeBoundingBox(routeLegs) else null
        if (bounds != null) {
            cameraState.animateTo(
                boundingBox = bounds,
                padding = PaddingValues(
                    start = 28.dp,
                    top = cameraTopPadding + 16.dp,
                    end = 28.dp,
                    bottom = cameraBottomPadding + 16.dp,
                ),
            )
            return@LaunchedEffect
        }
        if (!followCenter) {
            previousCenter = center
            previousFollowCenter = false
            return@LaunchedEffect
        }
        val finalPosition = CameraPosition(
            target = Position(longitude = target.longitude, latitude = target.latitude),
            zoom = DefaultZoom,
            bearing = cameraState.position.bearing,
            tilt = cameraState.position.tilt,
        )
        val isFirstRealCenter = previousCenter == null && center != null
        val paddingOnlyChange = previousCenter == center && previousFollowCenter
        val resumedFollow = followCenter && !previousFollowCenter
        previousCenter = center
        previousFollowCenter = followCenter
        if (isFirstRealCenter || (paddingOnlyChange && !resumedFollow)) {
            cameraState.animateTo(finalPosition = finalPosition, duration = 0.milliseconds)
        } else {
            cameraState.animateTo(finalPosition = finalPosition)
        }
    }

    val originDotColor = MaterialTheme.colorScheme.primary
    val originData = remember(origin?.latitude, origin?.longitude) {
        pointGeoJson(origin)
    }
    val destinationData = remember(destination?.latitude, destination?.longitude) {
        pointGeoJson(destination)
    }
    val walkColor = if (darkTheme) Color(0xFFD1D1D6) else Color(0xFF6E6E73)
    val casingColor = if (darkTheme) Color(0xFF1C1C1E) else Color.White
    val userPosition = remember(userLocation?.latitude, userLocation?.longitude) {
        userLocation?.let { Position(longitude = it.longitude, latitude = it.latitude) }
    }

    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(styleUri),
        cameraState = cameraState,
        cameraPadding = contentPadding,
        contentWindowInsets = WindowInsets(
            top = cameraTopPadding,
            bottom = cameraBottomPadding,
        ),
        options = MapOptions(
            gestureOptions = GestureOptions.Standard,
        ),
        overlay = MapOverlay {
            if (userPosition != null) {
                UserLocationMarker(
                    modifier = Modifier.placedAt(userPosition, Alignment.Center),
                )
            }
            if (!followCenter && !fitRoute) {
                MapCenterMarker(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        },
    ) {
        routeLegs.forEachIndexed { index, leg ->
            key("route-leg-$index") {
                val positions = remember(leg) { legPositions(leg) }
                if (positions.size >= 2) {
                    val source = rememberGeoJsonSource(data = lineGeoJson(positions))
                    val street = leg.mode.isStreetMode()
                    val lineColor = if (street) {
                        walkColor
                    } else {
                        parseHexColor(leg.routeColor) ?: MaterialTheme.colorScheme.primary
                    }
                    LineLayer(
                        id = "route-leg-casing-$index",
                        source = source,
                        color = const(casingColor),
                        width = const(if (street) 6.dp else 8.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round),
                    )
                    if (street) {
                        LineLayer(
                            id = "route-leg-$index",
                            source = source,
                            color = const(lineColor),
                            width = const(3.dp),
                            cap = const(LineCap.Round),
                            join = const(LineJoin.Round),
                            dasharray = const(listOf(1.2, 1.6)),
                        )
                    } else {
                        LineLayer(
                            id = "route-leg-$index",
                            source = source,
                            color = const(lineColor),
                            width = const(5.dp),
                            cap = const(LineCap.Round),
                            join = const(LineJoin.Round),
                        )
                    }
                }
            }
        }

        if (origin != null) {
            val originSource = rememberGeoJsonSource(data = originData)
            CircleLayer(
                id = "trip-origin",
                source = originSource,
                color = const(originDotColor),
                radius = const(7.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
        }
        if (destination != null) {
            val destinationSource = rememberGeoJsonSource(data = destinationData)
            CircleLayer(
                id = "trip-destination",
                source = destinationSource,
                color = const(MaterialTheme.colorScheme.error),
                radius = const(7.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
        }
    }
}

internal fun routeBoundingBox(legs: List<JourneyLeg>): BoundingBox? {
    val points = legs.flatMap { legPositions(it) }
    if (points.isEmpty()) return null
    var west = points.minOf { it.longitude }
    var east = points.maxOf { it.longitude }
    var south = points.minOf { it.latitude }
    var north = points.maxOf { it.latitude }
    if (west == east) {
        west -= 0.002
        east += 0.002
    }
    if (south == north) {
        south -= 0.002
        north += 0.002
    }
    return BoundingBox(west = west, south = south, east = east, north = north)
}

private fun legPositions(leg: JourneyLeg): List<Position> {
    val decoded = leg.geometry?.decodeCoordinates().orEmpty()
    val coordinates = decoded.ifEmpty {
        listOf(leg.from.coordinate, leg.to.coordinate)
    }
    return coordinates.map { coordinate ->
        Position(longitude = coordinate.longitude, latitude = coordinate.latitude)
    }
}

private fun lineGeoJson(positions: List<Position>): GeoJsonData =
    GeoJsonData.Features(
        Feature(
            geometry = LineString(positions),
            properties = null,
        ),
    )

private fun pointGeoJson(coordinate: Coordinate?): GeoJsonData =
    if (coordinate == null) {
        GeoJsonData.Features(FeatureCollection<Geometry, JsonObject?>(emptyList()))
    } else {
        GeoJsonData.Features(
            Feature(
                geometry = Point(
                    longitude = coordinate.longitude,
                    latitude = coordinate.latitude,
                ),
                properties = null,
            ),
        )
    }
