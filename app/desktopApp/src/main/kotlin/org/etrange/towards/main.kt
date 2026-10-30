package org.etrange.towards

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.etrange.towards.ui.window.DesktopTitleBar
import org.etrange.towards.ui.window.LocalDesktopTitleBar
import org.maplibre.compose.desktop.ProvideMapPresentationHost
import org.maplibre.compose.desktop.rememberAwtComposeMapPresentationHost

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 900.dp)
    val macOs = isMacOs()
    SwingWindow(
        onCloseRequest = ::exitApplication,
        title = "Towards",
        state = windowState,
        init = { frame ->
            if (macOs) frame.integrateMacOsTitleBar()
        },
    ) {
        val titleBar = if (macOs && windowState.placement != WindowPlacement.Fullscreen) {
            DesktopTitleBar.MacOs
        } else {
            DesktopTitleBar.None
        }
        ProvideMapPresentationHost(host = rememberAwtComposeMapPresentationHost(window)) {
            CompositionLocalProvider(LocalDesktopTitleBar provides titleBar) {
                App()
            }
        }
    }
}
