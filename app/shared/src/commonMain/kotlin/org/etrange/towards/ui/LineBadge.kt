package org.etrange.towards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
) {
    val background = parseHexColor(routeColor) ?: MaterialTheme.colorScheme.secondaryContainer
    val foreground = parseHexColor(routeTextColor) ?: MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier.background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
fun LineBadgePreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LineBadge(label = "3", routeColor = "FFDD00", routeTextColor = "000000")
    }
}
