package org.etrange.towards

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.maplibre.compose.desktop.ProvideMapPresentationHost
import org.maplibre.compose.desktop.rememberAwtComposeMapPresentationHost

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Towards",
    ) {
        ProvideMapPresentationHost(host = rememberAwtComposeMapPresentationHost(window)) {
            App()
        }
    }
}