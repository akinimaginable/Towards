package org.etrange.towards.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
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
private const val DefaultZoom = 15.0

@Composable
fun HomeMap(
    center: Coordinate?,
    modifier: Modifier = Modifier,
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
            padding = contentPadding,
        ),
    )
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val styleUri = if (darkTheme) {
        Res.getUri("files/towards_map_dark.json")
    } else {
        Res.getUri("files/towards_map_light.json")
    }
    var previousCenter by remember { mutableStateOf(center) }

    LaunchedEffect(target.latitude, target.longitude, contentPadding) {
        val finalPosition = cameraState.position.copy(
            target = Position(longitude = target.longitude, latitude = target.latitude),
            zoom = DefaultZoom,
            padding = contentPadding,
        )
        val isFirstRealCenter = previousCenter == null && center != null
        previousCenter = center
        if (isFirstRealCenter) {
            // Jump instantly so we don't animate from the Brussels default across the map.
            cameraState.animateTo(finalPosition = finalPosition, duration = 0.milliseconds)
        } else {
            cameraState.animateTo(finalPosition = finalPosition)
        }
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
