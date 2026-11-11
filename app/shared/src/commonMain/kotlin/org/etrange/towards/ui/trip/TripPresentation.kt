package org.etrange.towards.ui.trip

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.TripPlan
import kotlin.time.Clock
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
    return formatTripClock(instant, timeZone)
}

fun TripTime.headerLabel(
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String = when (this) {
    TripTime.LeaveNow -> "Leave now"
    is TripTime.DepartAt -> "Depart at ${formatTripMoment(instant, now, timeZone)}"
    is TripTime.ArriveBy -> "Arrive by ${formatTripMoment(instant, now, timeZone)}"
}

internal fun formatTripClock(instant: Instant, timeZone: TimeZone): String =
    clock24HourFormat.format(instant.toLocalDateTime(timeZone).time)

internal fun formatTripDate(
    instant: Instant,
    timeZone: TimeZone,
    now: Instant = Clock.System.now(),
): String {
    val date = instant.toLocalDateTime(timeZone).date
    val label = "${weekdayLabel(date.dayOfWeek)} ${date.day} ${monthLabel(date.month)}"
    val currentYear = now.toLocalDateTime(timeZone).year
    return if (date.year == currentYear) label else "$label ${date.year}"
}

private fun formatTripMoment(instant: Instant, now: Instant, timeZone: TimeZone): String {
    val local = instant.toLocalDateTime(timeZone)
    val clock = formatTripClock(instant, timeZone)
    if (local.date == now.toLocalDateTime(timeZone).date) return clock
    return "${formatTripDate(instant, timeZone, now)}, $clock"
}

private fun weekdayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

private fun monthLabel(month: Month): String = when (month) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
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
