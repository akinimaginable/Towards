package org.etrange.towards.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Both window edges must be at least this long before the side-by-side layout is used. */
internal val HorizontalLayoutMinSize = 600.dp

private val HorizontalPanelMinWidth = 320.dp
private val HorizontalPanelMaxWidth = 420.dp
private const val HorizontalPanelFraction = 0.42f

/** Forms without a map stay readable instead of stretching across a monitor. */
internal val ReadableContentMaxWidth = 560.dp

/**
 * Tablets and computer windows. A phone stays stacked, including when it is held sideways:
 * the short edge is still too small for a side panel.
 */
internal fun isHorizontalLayout(width: Dp, height: Dp): Boolean =
    width >= HorizontalLayoutMinSize && height >= HorizontalLayoutMinSize

/** About 42% of [width], clamped so the panel stays usable and the map keeps the rest. */
internal fun horizontalPanelWidth(width: Dp): Dp =
    (width * HorizontalPanelFraction).coerceIn(HorizontalPanelMinWidth, HorizontalPanelMaxWidth)

/**
 * On a wide window, centers [content] and caps it at [ReadableContentMaxWidth].
 * On a phone, [content] fills the available space.
 */
@Composable
internal fun ReadableContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = isHorizontalLayout(maxWidth, maxHeight)
        val contentWidth = ReadableContentMaxWidth.coerceAtMost(maxWidth)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (wide) Modifier.width(contentWidth) else Modifier.fillMaxWidth(),
                    ),
            ) {
                content()
            }
        }
    }
}
