package org.etrange.towards.data.desktop

internal fun desktopLocation(): DesktopLocation = DesktopLocations.shared

internal fun macDesktopLocation(): DesktopLocation {
    val native = loadNativeDesktopLocation("libtowards-location.dylib")
    if (native is NativeDesktopLocation && native.hostDeclaresLocationUsage()) {
        return native
    }
    return MacOsBundleLocation.install() ?: native
}

private object DesktopLocations {
    val shared: DesktopLocation by lazy { create() }

    private fun create(): DesktopLocation {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            os.startsWith("mac") -> macDesktopLocation()
            os.startsWith("windows") -> loadNativeDesktopLocation("towards-location.dll")
            os.startsWith("linux") -> linuxPortalLocation()
            else -> UnavailableLocation
        }
    }
}
