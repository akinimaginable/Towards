package org.etrange.towards.ui.trip

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.etrange.towards.data.toApiDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** When the trip should be planned. Leave now is resolved at each search. */
sealed interface TripTime {
    data object LeaveNow : TripTime

    data class DepartAt(val instant: Instant) : TripTime

    data class ArriveBy(val instant: Instant) : TripTime
}

/** Epoch seconds of a scheduled search. Null means leave now. */
fun TripTime.epochSeconds(): Long? = when (this) {
    TripTime.LeaveNow -> null
    is TripTime.DepartAt -> instant.epochSeconds
    is TripTime.ArriveBy -> instant.epochSeconds
}

fun TripTime.arrivesBy(): Boolean = this is TripTime.ArriveBy

fun tripTimeOf(epochSeconds: Long?, arriveBy: Boolean): TripTime {
    val seconds = epochSeconds ?: return TripTime.LeaveNow
    val instant = Instant.fromEpochSeconds(seconds)
    return if (arriveBy) TripTime.ArriveBy(instant) else TripTime.DepartAt(instant)
}

/** Time string and arrive-by flag for [org.etrange.towards.domain.model.requests.TripPlanningRequest]. */
fun TripTime.toRequestTime(now: Instant = Clock.System.now()): Pair<String, Boolean> = when (this) {
    TripTime.LeaveNow -> now.toApiDateTime() to false
    is TripTime.DepartAt -> instant.toApiDateTime() to false
    is TripTime.ArriveBy -> instant.toApiDateTime() to true
}

/** The instant shown for a scheduled search, or the next 5-minute mark when leaving now. */
fun TripTime.scheduledInstant(
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Instant = when (this) {
    TripTime.LeaveNow -> nextFiveMinuteMark(now, timeZone)
    is TripTime.DepartAt -> instant
    is TripTime.ArriveBy -> instant
}

/**
 * The next 5-minute boundary at or after [now]. An exact boundary is kept; any seconds past it
 * move to the following one. The step is applied on the instant timeline, so a daylight-saving
 * transition moves the local clock with the elapsed minutes.
 */
fun nextFiveMinuteMark(now: Instant, timeZone: TimeZone): Instant {
    val local = now.toLocalDateTime(timeZone)
    if (local.second == 0 && local.nanosecond == 0 && local.minute % 5 == 0) return now
    val minutesToAdd = if (local.minute % 5 == 0) 5 else 5 - (local.minute % 5)
    val startOfMinute = now
        .minus(local.second, DateTimeUnit.SECOND, timeZone)
        .minus(local.nanosecond, DateTimeUnit.NANOSECOND, timeZone)
    return startOfMinute.plus(minutesToAdd, DateTimeUnit.MINUTE, timeZone)
}

/** UTC start-of-day millis for the calendar date of [instant] in [timeZone]. */
fun utcDayMillisOf(instant: Instant, timeZone: TimeZone): Long {
    val date = instant.toLocalDateTime(timeZone).date
    return utcStartMillis(date)
}

/**
 * Combines a Material3 calendar day with a local time.
 * [utcDayMillis] is the start of the selected calendar day in UTC, which is how
 * [androidx.compose.material3.DatePickerState.selectedDateMillis] is defined.
 */
fun instantFromCalendarDayAndLocalTime(
    utcDayMillis: Long,
    hour: Int,
    minute: Int,
    timeZone: TimeZone,
): Instant {
    val date = Instant.fromEpochMilliseconds(utcDayMillis).toLocalDateTime(TimeZone.UTC).date
    return LocalDateTime(date, LocalTime(hour, minute)).toInstant(timeZone)
}

private fun utcStartMillis(date: LocalDate): Long =
    LocalDateTime(date, LocalTime(0, 0)).toInstant(TimeZone.UTC).toEpochMilliseconds()
