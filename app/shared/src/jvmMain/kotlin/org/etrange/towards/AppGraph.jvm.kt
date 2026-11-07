package org.etrange.towards

import org.etrange.towards.data.JvmLocationProvider
import org.etrange.towards.data.desktop.desktopLocation

fun createAppGraph(): AppGraph = AppGraph(JvmLocationProvider(desktopLocation()))
