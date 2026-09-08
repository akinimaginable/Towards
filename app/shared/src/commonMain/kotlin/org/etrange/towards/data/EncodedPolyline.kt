package org.etrange.towards.data

import kotlin.math.pow
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.EncodedPath

fun EncodedPath.decodeCoordinates(): List<Coordinate> = decodePolyline(points, precision)

fun decodePolyline(encoded: String, precision: Int): List<Coordinate> {
    if (encoded.isEmpty()) return emptyList()
    val factor = 10.0.pow(precision.coerceAtLeast(0))
    val coordinates = ArrayList<Coordinate>()
    var index = 0
    var latitude = 0
    var longitude = 0

    try {
        while (index < encoded.length) {
            val latitudeChange = decodeNext(encoded, index)
            index = latitudeChange.index
            latitude += latitudeChange.value
            val longitudeChange = decodeNext(encoded, index)
            index = longitudeChange.index
            longitude += longitudeChange.value
            coordinates += Coordinate(
                latitude = latitude / factor,
                longitude = longitude / factor,
            )
        }
    } catch (_: IndexOutOfBoundsException) {
        return coordinates
    }

    return coordinates
}

private data class DecodedDelta(val value: Int, val index: Int)

private fun decodeNext(encoded: String, start: Int): DecodedDelta {
    var result = 0
    var shift = 0
    var index = start
    var byte: Int
    do {
        byte = encoded[index++].code - 63
        result = result or ((byte and 0x1f) shl shift)
        shift += 5
    } while (byte >= 0x20)
    val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
    return DecodedDelta(value, index)
}
