package org.etrange.towards.ui.trip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.etrange.towards.ui.icons.arrow_backIcon
import org.etrange.towards.ui.icons.swapVertIcon
import org.etrange.towards.ui.search.DestinationMarker
import org.etrange.towards.ui.search.OriginMarker
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

/**
 * Header of the trip results screen. It sits on a solid surface above the map (never over map
 * tiles) and shows both endpoints as tappable rows with the same markers as the place search.
 */
@Composable
fun TripHeader(
    originName: String,
    destinationName: String,
    onBack: () -> Unit,
    onSwap: () -> Unit,
    onChangeOrigin: () -> Unit,
    onChangeDestination: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = arrow_backIcon,
                        contentDescription = "Back",
                    )
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column {
                        EndpointRow(
                            marker = { OriginMarker() },
                            text = originName,
                            actionLabel = "Change starting point",
                            onClick = onChangeOrigin,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 48.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        EndpointRow(
                            marker = { DestinationMarker() },
                            text = destinationName,
                            actionLabel = "Change destination",
                            onClick = onChangeDestination,
                            emphasized = true,
                        )
                    }
                }
                IconButton(onClick = onSwap) {
                    Icon(
                        imageVector = swapVertIcon,
                        contentDescription = "Swap origin and destination",
                    )
                }
            }
            Text(
                text = "Leaving now",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 68.dp, bottom = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun EndpointRow(
    marker: @Composable () -> Unit,
    text: String,
    actionLabel: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        marker()
        Text(
            text = text,
            style = if (emphasized) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun TripHeaderPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        TripHeader(
            originName = "My location",
            destinationName = "Grand Place",
            onBack = {},
            onSwap = {},
            onChangeOrigin = {},
            onChangeDestination = {},
        )
    }
}

@Preview
@Composable
private fun TripHeaderDarkPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        TripHeader(
            originName = "My location",
            destinationName = "Bruxelles-Central / Brussel-Centraal Train Station",
            onBack = {},
            onSwap = {},
            onChangeOrigin = {},
            onChangeDestination = {},
        )
    }
}
