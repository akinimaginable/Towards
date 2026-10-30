package org.etrange.towards

import androidx.compose.ui.awt.ComposeWindow
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

internal fun isMacOs(): Boolean =
    System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)

/**
 * Draws the window content under a transparent title bar and hides the title text.
 * The traffic lights stay, and AppKit still hit-tests them above the Swing content.
 * Must run before the window is shown.
 */
internal fun ComposeWindow.integrateMacOsTitleBar() {
    rootPane.putClientProperty("apple.awt.fullWindowContent", true)
    rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
    rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
    addWindowListener(object : WindowAdapter() {
        override fun windowOpened(e: WindowEvent) {
            removeWindowListener(this)
            MacOsWindowDrag.makeOpaque(this@integrateMacOsTitleBar)
            // Compose sees the press only after mouseDown returns, which is too late for
            // performWindowDragWithEvent. Hook sendEvent: so AppKit tracks the drag.
            MacOsWindowDrag.installDragQueue(this@integrateMacOsTitleBar)
        }
    })
}
