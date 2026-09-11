package org.etrange.towards.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val liveIcon: ImageVector
    get() {
        if (_live != null) {
            return _live!!
        }
        _live = ImageVector.Builder(
            name = "live",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(6.5f, 14.5f)
                curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
                reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                close()
                moveTo(4f, 4.71f)
                verticalLineToRelative(2.1f)
                curveTo(11.19f, 6.81f, 17f, 12.62f, 17f, 19.81f)
                horizontalLineToRelative(2.1f)
                curveTo(19.1f, 11.47f, 12.53f, 4.71f, 4f, 4.71f)
                close()
                moveTo(4f, 9.92f)
                verticalLineToRelative(2.12f)
                curveTo(8.28f, 12.04f, 11.78f, 15.53f, 11.78f, 19.82f)
                horizontalLineToRelative(2.1f)
                curveTo(13.89f, 14.47f, 9.47f, 9.92f, 4f, 9.92f)
                close()
            }
        }.build()
        return _live!!
    }

private var _live: ImageVector? = null
