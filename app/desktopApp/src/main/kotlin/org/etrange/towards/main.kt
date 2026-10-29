package org.etrange.towards

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.maplibre.compose.desktop.ProvideMapPresentationHost
import org.maplibre.compose.desktop.rememberAwtComposeMapPresentationHost

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Towards",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        ProvideMapPresentationHost(host = rememberAwtComposeMapPresentationHost(window)) {
            App()
        }
    }
}