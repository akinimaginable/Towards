package org.etrange.towards

import android.app.Application
import org.etrange.towards.data.AndroidLocationProvider

class TowardsApplication : Application() {
    val graph by lazy { AppGraph(AndroidLocationProvider(this)) }
}
