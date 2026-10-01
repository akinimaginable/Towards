package org.etrange.towards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun LineBadge(
    label: String,
    routeColor: String?,
    routeTextColor: String?,
    modifier: Modifier = Modifier,
) {
    val routeBackground = parseHexColor(routeColor)
    val background = routeBackground ?: MaterialTheme.colorScheme.secondaryContainer
    val foreground = parseHexColor(routeTextColor)
        ?: routeBackground?.let(::readableTextColorOn)
        ?: MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 32.dp, minHeight = 24.dp)
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

/** Black or white, whichever contrasts more with [background]. */
internal fun readableTextColorOn(background: Color): Color =
    if (background.luminance() > 0.179f) Color.Black else Color.White

@Preview
@Composable
fun LineBadgePreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LineBadge(label = "3", routeColor = "FFDD00", routeTextColor = "000000")
    }
}
