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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import org.etrange.towards.domain.model.Coordinate
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlinx.serialization.json.JsonObject
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
    val cameraTopPadding = contentPadding.calculateTopPadding()
    val cameraBottomPadding = contentPadding.calculateBottomPadding()

    LaunchedEffect(target.latitude, target.longitude, cameraTopPadding, cameraBottomPadding) {
        cameraState.awaitViewport()
        val finalPosition = CameraPosition(
            target = Position(longitude = target.longitude, latitude = target.latitude),
            zoom = DefaultZoom,
            bearing = cameraState.position.bearing,
            tilt = cameraState.position.tilt,
        )
        val isFirstRealCenter = previousCenter == null && center != null
        val paddingOnlyChange = previousCenter == center
        previousCenter = center
        if (isFirstRealCenter || paddingOnlyChange) {
            cameraState.animateTo(finalPosition = finalPosition, duration = 0.milliseconds)
        } else {
            cameraState.animateTo(finalPosition = finalPosition)
        }
    }

    val locationDotColor = MaterialTheme.colorScheme.primary
    val locationHaloColor = locationDotColor.copy(alpha = 0.22f)
    val userLocationData = remember(userLocation?.latitude, userLocation?.longitude) {
        userLocationGeoJson(userLocation)
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
        overlay = MapOverlay.None,
    ) {
        val userLocationSource = rememberGeoJsonSource(data = userLocationData)
        CircleLayer(
            id = "user-location-halo",
            source = userLocationSource,
            color = const(locationHaloColor),
            radius = const(16.dp),
            strokeWidth = const(0.dp),
        )
        CircleLayer(
            id = "user-location",
            source = userLocationSource,
            color = const(locationDotColor),
            radius = const(6.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp),
        )
    }
}

private fun userLocationGeoJson(userLocation: Coordinate?): GeoJsonData =
    if (userLocation == null) {
        GeoJsonData.Features(FeatureCollection<Geometry, JsonObject?>(emptyList()))
    } else {
        GeoJsonData.Features(
            Feature(
                geometry = Point(
                    longitude = userLocation.longitude,
                    latitude = userLocation.latitude,
                ),
                properties = null,
            ),
        )
    }
