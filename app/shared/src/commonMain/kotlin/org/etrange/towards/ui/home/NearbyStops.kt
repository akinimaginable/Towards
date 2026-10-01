package org.etrange.towards.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.etrange.towards.ui.LineBadge
import org.etrange.towards.ui.icons.liveIcon
import org.etrange.towards.ui.preview.previewNearbyStops
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Screen edge inset shared by stop headers, mode labels and departure rows. */
private val ListInset = 16.dp
private val BadgeMinWidth = 32.dp
private val BadgeGap = 12.dp

fun LazyListScope.nearbyStopsSection(
    nearbyStops: List<NearbyStop>,
    isLoadingNearby: Boolean,
    nearbyMessage: String?,
    onStopClick: (NearbyStop) -> Unit = {},
) {
    if (isLoadingNearby && nearbyStops.isEmpty()) {
        item(key = "nearby-loading") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        }
        return
    }

    if (nearbyMessage != null && nearbyStops.isEmpty()) {
        item(key = "nearby-message") {
            Text(
                text = nearbyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ListInset, vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    nearbyStops.forEachIndexed { stopIndex, stop ->
        item(key = "stop-header-$stopIndex-${stop.id}") {
            NearbyStopHeader(stop = stop, onClick = { onStopClick(stop) })
        }
        val groups = stop.departuresByMode()
        groups.forEach { (group, departures) ->
            if (groups.size > 1) {
                item(key = "mode-$stopIndex-${stop.id}-$group") {
                    ModeGroupLabel(group)
                }
            }
            itemsIndexed(
                items = departures,
                key = { departureIndex, departure ->
                    "dep-$stopIndex-$group-$departureIndex-${departure.id}"
                },
            ) { departureIndex, departure ->
                NearbyDepartureRow(
                    departure = departure,
                    showDivider = departureIndex < departures.lastIndex,
                )
            }
        }
    }
}

@Composable
fun NearbyStopHeader(stop: NearbyStop, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Directions to ${stop.name}",
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { heading() }
            .padding(start = ListInset, end = ListInset, top = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stop.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDistanceMeters(stop.distanceMeters),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeGroupLabel(group: ModeGroup) {
    Text(
        text = group.label().uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(start = ListInset, end = ListInset, top = 4.dp, bottom = 2.dp),
    )
}

@Composable
fun NearbyDepartureRow(departure: NearbyDeparture, showDivider: Boolean = false) {
    val now by produceState(initialValue = Clock.System.now(), key1 = departure.id) {
        while (isActive) {
            value = Clock.System.now()
            delay(30.seconds)
        }
    }
    val headsign = departure.headsign.orEmpty().toDisplayCase()
    val countdown = countdownOf(departure.time, now)
    val laterLabel = laterTimesLabel(departure.laterTimes, now)
    val description = buildString {
        append("Line ${departure.lineName}")
        if (headsign.isNotBlank()) append(" to $headsign")
        append(", ${countdown.spoken}")
        if (departure.realTime) append(", live")
        if (laterLabel != null) append(", $laterLabel")
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clearAndSetSemantics { contentDescription = description }
                .padding(horizontal = ListInset, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(BadgeGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LineBadge(
                label = departure.lineName,
                routeColor = departure.routeColor,
                routeTextColor = departure.routeTextColor,
            )
            Text(
                text = headsign,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DepartureCountdown(
                countdown = countdown,
                realTime = departure.realTime,
                laterLabel = laterLabel,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = ListInset + BadgeMinWidth + BadgeGap),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun DepartureCountdown(countdown: Countdown, realTime: Boolean, laterLabel: String?) {
    val accent = MaterialTheme.colorScheme.primary
    val emphasized = realTime || countdown.isNow
    Column(horizontalAlignment = Alignment.End) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (realTime) {
                Icon(
                    imageVector = liveIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(end = 1.dp),
                    tint = accent,
                )
            }
            Text(
                text = countdown.value,
                style = if (countdown.isNow) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                }.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = if (emphasized) accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            if (countdown.unit != null) {
                Text(
                    text = countdown.unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        if (laterLabel != null) {
            Text(
                text = laterLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class Countdown(val value: String, val unit: String?, val spoken: String) {
    val isNow: Boolean get() = unit == null && value == "now"
}

private val clockFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

private fun Instant.clockLabel(): String =
    clockFormat.format(toLocalDateTime(TimeZone.currentSystemDefault()).time)

/** Minutes for the next hour, then a clock time, which is easier to read than "1 h 12". */
private fun countdownOf(time: Instant, now: Instant): Countdown {
    val minutes = minutesUntil(time, now) ?: return Countdown("now", null, "now")
    if (minutes >= 60) {
        val clock = time.clockLabel()
        return Countdown(clock, null, "at $clock")
    }
    return Countdown(minutes.toString(), "min", "in $minutes min")
}

private fun laterTimesLabel(times: List<Instant>, now: Instant): String? {
    val minutes = times.mapNotNull { minutesUntil(it, now) }.filter { it < 60 }
    if (minutes.isEmpty()) return null
    return "then ${minutes.joinToString(", ")} min"
}

private fun ModeGroup.label(): String = when (this) {
    ModeGroup.METRO -> "Metro"
    ModeGroup.TRAIN -> "Train"
    ModeGroup.TRAM -> "Tram"
    ModeGroup.BUS -> "Bus"
    ModeGroup.OTHER -> "Other"
}

@Preview
@Composable
fun NearbyStopsSectionPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LazyColumn {
            nearbyStopsSection(
                nearbyStops = previewNearbyStops(),
                isLoadingNearby = false,
                nearbyMessage = null,
            )
        }
    }
}

@Preview
@Composable
fun NearbyStopsSectionDarkPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        LazyColumn {
            nearbyStopsSection(
                nearbyStops = previewNearbyStops(),
                isLoadingNearby = false,
                nearbyMessage = null,
            )
        }
    }
}

@Preview
@Composable
fun NearbyStopHeaderPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        NearbyStopHeader(stop = previewNearbyStops().first())
    }
}

@Preview
@Composable
fun NearbyDepartureRowPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        NearbyDepartureRow(departure = previewNearbyStops().first().departures.first())
    }
}

