package org.etrange.towards

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform