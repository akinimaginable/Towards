package org.etrange.towards.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal val UserLocationMarkerColor = Color(0xFF0088FF)
internal val MapCenterMarkerColor = Color(0xFF612DF5)
internal val MarkerSize = 32.dp

@Composable
private fun LocationMarker(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(MarkerSize)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
        )
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape)
                .background(color)
                .border(width = 2.dp, color = Color.White, shape = CircleShape),
        )
    }
}

@Composable
fun UserLocationMarker(modifier: Modifier = Modifier) {
    LocationMarker(color = UserLocationMarkerColor, modifier = modifier)
}

@Composable
fun MapCenterMarker(modifier: Modifier = Modifier) {
    LocationMarker(color = MapCenterMarkerColor, modifier = modifier)
}

@Preview
@Composable
fun UserLocationMarkerPreview() {
    UserLocationMarker()
}

@Preview
@Composable
fun MapCenterMarkerPreview() {
    MapCenterMarker()
}
