package org.etrange.towards.ui

import androidx.compose.ui.graphics.Color

internal fun parseHexColor(value: String?): Color? {
    if (value.isNullOrBlank()) return null
    val hex = value.removePrefix("#")
    val normalized = when (hex.length) {
        6 -> "FF$hex"
        8 -> hex
        else -> return null
    }
    return runCatching { Color(normalized.toLong(16)) }.getOrNull()
}
