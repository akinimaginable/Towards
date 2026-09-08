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
val swapVertIcon: ImageVector
    get() {
        if (_swapVert != null) {
            return _swapVert!!
        }
        _swapVert = ImageVector.Builder(
            name = "swap_vert",
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
                moveTo(16f, 17.01f)
                verticalLineTo(10f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(7.01f)
                horizontalLineToRelative(-3f)
                lineTo(15f, 21f)
                lineToRelative(4f, -3.99f)
                horizontalLineToRelative(-3f)
                close()
                moveTo(9f, 3f)
                lineTo(5f, 6.99f)
                horizontalLineToRelative(3f)
                verticalLineTo(14f)
                horizontalLineToRelative(2f)
                verticalLineTo(6.99f)
                horizontalLineToRelative(3f)
                lineTo(9f, 3f)
                close()
            }
        }.build()
        return _swapVert!!
    }

private var _swapVert: ImageVector? = null
