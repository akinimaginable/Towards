package org.etrange.towards.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.etrange.towards.domain.model.Coordinate
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import towards.app.shared.generated.resources.Res

private val DefaultMapCenter = Coordinate(latitude = 50.8503, longitude = 4.3517)
private const val DefaultZoom = 14.0

@Composable
fun HomeMap(
    center: Coordinate?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val target = center ?: DefaultMapCenter
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = target.longitude, latitude = target.latitude),
            zoom = DefaultZoom,
            padding = contentPadding,
        ),
    )
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val styleUri = if (darkTheme) {
        Res.getUri("files/towards_map_dark.json")
    } else {
        Res.getUri("files/towards_map_light.json")
    }

    LaunchedEffect(target.latitude, target.longitude, contentPadding) {
        cameraState.animateTo(
            finalPosition = cameraState.position.copy(
                target = Position(longitude = target.longitude, latitude = target.latitude),
                zoom = DefaultZoom,
                padding = contentPadding,
            ),
        )
    }

    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(styleUri),
        cameraState = cameraState,
        options = MapOptions(
            gestureOptions = GestureOptions.Standard,
            ornamentOptions = OrnamentOptions.OnlyLogo,
        ),
    )
}
