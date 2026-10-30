package org.etrange.towards.ui.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Space the desktop window reserves when its native title bar is drawn over the content.
 * [None] on phones, and on desktop windows that still use a normal frame.
 * Traffic lights only occupy the top band, so content below that band keeps its usual left inset.
 */
data class DesktopTitleBar(
    /** Height of the band the traffic lights occupy. */
    val height: Dp = 0.dp,
) {
    val isIntegrated: Boolean get() = height > 0.dp

    companion object {
        val None = DesktopTitleBar()

        /** macOS traffic lights sit in a 28pt band. */
        val MacOs = DesktopTitleBar(height = 28.dp)
    }
}

val LocalDesktopTitleBar = compositionLocalOf { DesktopTitleBar.None }

/** Top app bar insets, plus the traffic-light band when the title bar is integrated. */
@Composable
internal fun titleBarWindowInsets(): WindowInsets {
    val bar = LocalDesktopTitleBar.current
    val base = TopAppBarDefaults.windowInsets
    return if (bar.isIntegrated) base.union(WindowInsets(top = bar.height)) else base
}

/** Pushes a block down so it starts below the traffic lights. */
@Composable
internal fun Modifier.titleBarTopPadding(): Modifier {
    val height = LocalDesktopTitleBar.current.height
    return if (height > 0.dp) padding(top = height) else this
}
