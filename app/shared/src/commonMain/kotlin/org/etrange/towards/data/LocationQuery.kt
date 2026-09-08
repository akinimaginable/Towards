package org.etrange.towards.data

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.LocationReference

internal fun Coordinate.toQueryParameter(): String = buildString {
    append(latitude)
    append(',')
    append(longitude)
    level?.let {
        append(',')
        append(it)
    }
}

internal fun LocationReference.toQueryParameter(): String = when (this) {
    is LocationReference.Stop -> id
    is LocationReference.Position -> coordinate.toQueryParameter()
}
