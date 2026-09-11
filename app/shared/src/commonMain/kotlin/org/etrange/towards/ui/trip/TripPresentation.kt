package org.etrange.towards.ui.trip

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.TripPlan
import kotlin.time.Instant

private val clock24HourFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

fun combineTripOptions(plan: TripPlan): List<Itinerary> {
    val seen = mutableSetOf<String>()
    return (plan.itineraries + plan.direct).mapIndexed { index, itinerary ->
        if (itinerary.id.isNotBlank()) {
            itinerary
        } else {
            itinerary.copy(id = "itinerary-$index-${itinerary.startTime}")
        }
    }.filter { itinerary -> seen.add(itinerary.id) }
}

fun formatDuration(seconds: Int): String {
    val totalMinutes = (seconds / 60).coerceAtLeast(0)
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0) "$hours h" else "$hours h $minutes min"
}

fun formatClock(
    isoDateTime: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val instant = runCatching { Instant.parse(isoDateTime) }.getOrNull() ?: return isoDateTime
    return clock24HourFormat.format(instant.toLocalDateTime(timeZone).time)
}

fun Itinerary.walkSeconds(): Int =
    legs.filter { it.mode == TransportMode.WALK }.sumOf { it.durationSeconds }

fun Itinerary.isWalkOnly(): Boolean =
    legs.isNotEmpty() && legs.all { it.mode.isStreetMode() }

fun TransportMode.isStreetMode(): Boolean = when (this) {
    TransportMode.WALK,
    TransportMode.BIKE,
    TransportMode.CAR,
    TransportMode.CAR_PARKING,
    TransportMode.CAR_DROPOFF,
    -> true
    else -> false
}

fun Itinerary.summaryLabel(): String {
    val walkMinutes = walkSeconds() / 60
    return buildList {
        if (isWalkOnly()) {
            add("Walk")
        } else {
            if (walkMinutes > 0) add("Walk $walkMinutes min")
            when (transfers) {
                0 -> add("Direct")
                1 -> add("1 transfer")
                else -> add("$transfers transfers")
            }
        }
    }.joinToString(" · ")
}

fun JourneyLeg.lineLabel(): String =
    displayName?.takeIf { it.isNotBlank() }
        ?: mode.name.lowercase().replaceFirstChar { it.titlecase() }

fun JourneyLeg.delayMinutes(): Long? {
    val expected = runCatching { Instant.parse(startTime) }.getOrNull() ?: return null
    val scheduled = runCatching { Instant.parse(scheduledStartTime) }.getOrNull() ?: return null
    return (expected - scheduled).inWholeMinutes
}

fun JourneyLeg.realtimeStatusLabel(): String {
    if (cancelled) return "Cancelled"
    if (!realTime) return "Scheduled"
    val delay = delayMinutes() ?: return "Live"
    return when {
        delay > 0 -> "Live · $delay min late"
        delay < 0 -> "Live · ${-delay} min early"
        else -> "Live · on time"
    }
}

fun JourneyLeg.detailTitle(): String {
    if (mode.isStreetMode()) {
        val minutes = (durationSeconds / 60).coerceAtLeast(1)
        val action = when (mode) {
            TransportMode.BIKE -> "Bike"
            TransportMode.CAR, TransportMode.CAR_PARKING, TransportMode.CAR_DROPOFF -> "Drive"
            else -> "Walk"
        }
        return "$action $minutes min to ${to.name}"
    }
    return listOfNotNull(lineLabel(), headsign?.takeIf { it.isNotBlank() }).joinToString(" ")
}
