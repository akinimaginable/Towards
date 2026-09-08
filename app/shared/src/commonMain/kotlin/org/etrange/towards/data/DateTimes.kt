package org.etrange.towards.data

import kotlin.time.Instant

fun Instant.toApiDateTime(): String = Instant.fromEpochSeconds(epochSeconds).toString()
