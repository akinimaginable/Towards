package org.etrange.towards.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.etrange.towards.ui.icons.myLocationIcon
import org.etrange.towards.ui.icons.searchIcon
import org.etrange.towards.ui.theme.TowardsTheme

@Composable
fun SearchLaunchBar(
    onClick: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    followMap: Boolean,
    isLocating: Boolean,
    focusedPlaceLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val label = focusedPlaceLabel?.takeIf { it.isNotBlank() } ?: "Where to?"
    Surface(
        modifier = modifier.clip(RoundedCornerShape(32.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = if (followMap) onClick else onUseCurrentLocation,
                enabled = followMap || !isLocating,
                modifier = Modifier.size(48.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        isLocating -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        followMap -> Icon(
                            imageVector = searchIcon,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> Icon(
                            imageVector = myLocationIcon,
                            contentDescription = "Recenter on me",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 16.dp),
            )
        }
    }
}

@Preview
@Composable
fun SearchLaunchBarPreview() {
    TowardsTheme {
        SearchLaunchBar(
            onClick = {},
            onUseCurrentLocation = {},
            followMap = true,
            isLocating = false,
        )
    }
}

@Preview
@Composable
fun SearchLaunchBarFocusedPreview() {
    TowardsTheme {
        SearchLaunchBar(
            onClick = {},
            onUseCurrentLocation = {},
            followMap = false,
            isLocating = false,
            focusedPlaceLabel = "Grand Place, 1000, Belgium",
        )
    }
}
