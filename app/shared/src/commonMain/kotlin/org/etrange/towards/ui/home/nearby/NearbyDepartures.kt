package org.etrange.towards.ui.home.nearby

import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.nearby.NearbyDeparture
import org.etrange.towards.domain.model.nearby.NearbyStop
import org.etrange.towards.domain.model.nearby.TripDeparture

/** This departure followed by the later ones of the same line and direction. */
fun NearbyDeparture.upcomingTrips(): List<TripDeparture> =
    listOf(TripDeparture(tripId, time, realTime)) + laterTrips

/** How departures of one stop are split into labelled sections, in display order. */
enum class ModeGroup {
    METRO,
    TRAIN,
    TRAM,
    BUS,
    OTHER,
}

fun TransportMode.modeGroup(): ModeGroup = when (this) {
    TransportMode.SUBWAY -> ModeGroup.METRO
    TransportMode.RAIL,
    TransportMode.HIGHSPEED_RAIL,
    TransportMode.LONG_DISTANCE,
    TransportMode.NIGHT_RAIL,
    TransportMode.REGIONAL_FAST_RAIL,
    TransportMode.REGIONAL_RAIL,
    TransportMode.SUBURBAN,
    -> ModeGroup.TRAIN
    TransportMode.TRAM -> ModeGroup.TRAM
    TransportMode.BUS, TransportMode.COACH -> ModeGroup.BUS
    else -> ModeGroup.OTHER
}

fun NearbyStop.departuresByMode(): List<Pair<ModeGroup, List<NearbyDeparture>>> =
    departures.groupBy { it.mode.modeGroup() }.toList().sortedBy { (group, _) -> group.ordinal }

fun relativeLabel(time: Instant, now: Instant): String {
    val delta: Duration = time - now
    val totalMinutes = if (delta < 1.minutes) 0 else delta.inWholeMinutes
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "$hours h" else "$hours h $minutes"
}

/** Whole minutes until [time]. A departure within the current minute is zero. */
fun minutesUntil(time: Instant, now: Instant): Long {
    val delta = time - now
    return if (delta < 1.minutes) 0 else delta.inWholeMinutes
}

/** Minutes to walk [meters] at about 4.8 km/h, at least one. */
fun walkingMinutes(meters: Int): Int = ((meters + 79) / 80).coerceAtLeast(1)

fun formatDistanceMeters(meters: Int): String =
    if (meters < 1000) "$meters m" else "${(meters / 100.0).roundToInt() / 10.0} km"

/**
 * Converts all-caps feed text ("GARE DE L'OUEST") to title case ("Gare de l'Ouest").
 * Mixed-case text is returned unchanged, since it was already written by a person.
 */
fun String.toDisplayCase(): String {
    if (none { it.isLetter() } || any { it.isLowerCase() }) return this
    var wordIndex = 0
    return split(' ').joinToString(" ") { word ->
        val cased = word.titleCaseWord(isFirst = wordIndex == 0)
        if (word.isNotEmpty()) wordIndex++
        cased
    }
}

private val lowercaseParticles = setOf(
    "de", "du", "des", "la", "le", "les", "et", "en", "sur", "aux", "au",
    "ten", "van", "der", "den", "het", "op", "te",
)

private fun String.titleCaseWord(isFirst: Boolean): String {
    val lower = lowercase()
    val elision = lower.indexOf('\'')
    if (elision in 1..2) {
        val prefix = lower.substring(0, elision + 1)
        val rest = lower.substring(elision + 1).capitalizeParts()
        return if (isFirst) prefix.replaceFirstChar { it.titlecase() } + rest else prefix + rest
    }
    if (!isFirst && lower in lowercaseParticles) return lower
    return lower.capitalizeParts()
}

private fun String.capitalizeParts(): String =
    split('-').joinToString("-") { part -> part.replaceFirstChar { it.titlecase() } }
