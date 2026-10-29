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
val trainIcon: ImageVector
    get() {
        if (_train != null) {
            return _train!!
        }
        _train = ImageVector.Builder(
            name = "train",
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
                moveTo(4f, 15.5f)
                verticalLineTo(6f)
                quadTo(4f, 4.67f, 4.69f, 3.89f)
                reflectiveQuadTo(6.5f, 2.69f)
                quadTo(7.63f, 2.27f, 9.06f, 2.14f)
                reflectiveQuadTo(12f, 2f)
                quadToRelative(1.65f, 0f, 3.11f, 0.14f)
                reflectiveQuadToRelative(2.55f, 0.55f)
                reflectiveQuadToRelative(1.71f, 1.2f)
                quadTo(20f, 4.67f, 20f, 6f)
                verticalLineToRelative(9.5f)
                quadToRelative(0f, 1.48f, -1.01f, 2.49f)
                reflectiveQuadTo(16.5f, 19f)
                lineTo(18f, 20.5f)
                verticalLineTo(21f)
                horizontalLineTo(16f)
                lineTo(14f, 19f)
                horizontalLineTo(10f)
                lineTo(8f, 21f)
                horizontalLineTo(6f)
                verticalLineTo(20.5f)
                lineTo(7.5f, 19f)
                quadTo(6.03f, 19f, 5.01f, 17.99f)
                reflectiveQuadTo(4f, 15.5f)
                close()
                moveTo(12f, 4f)
                quadTo(9.35f, 4f, 8.13f, 4.31f)
                reflectiveQuadTo(6.45f, 5f)
                horizontalLineToRelative(11.2f)
                quadTo(17.28f, 4.57f, 16.04f, 4.29f)
                reflectiveQuadTo(12f, 4f)
                close()
                moveTo(6f, 10f)
                horizontalLineToRelative(5f)
                verticalLineTo(7f)
                horizontalLineTo(6f)
                verticalLineToRelative(3f)
                close()
                moveToRelative(10.5f, 2f)
                horizontalLineToRelative(-9f)
                quadTo(6.85f, 12f, 6.43f, 12f)
                reflectiveQuadTo(6f, 12f)
                horizontalLineTo(18f)
                quadToRelative(0f, 0f, -0.43f, 0f)
                reflectiveQuadTo(16.5f, 12f)
                close()
                moveTo(13f, 10f)
                horizontalLineToRelative(5f)
                verticalLineTo(7f)
                horizontalLineTo(13f)
                verticalLineToRelative(3f)
                close()
                moveTo(9.58f, 15.58f)
                quadTo(10f, 15.15f, 10f, 14.5f)
                reflectiveQuadTo(9.58f, 13.43f)
                reflectiveQuadTo(8.5f, 13f)
                reflectiveQuadTo(7.43f, 13.43f)
                reflectiveQuadTo(7f, 14.5f)
                reflectiveQuadToRelative(0.43f, 1.07f)
                reflectiveQuadTo(8.5f, 16f)
                reflectiveQuadTo(9.58f, 15.58f)
                close()
                moveToRelative(7f, 0f)
                quadTo(17f, 15.15f, 17f, 14.5f)
                reflectiveQuadTo(16.58f, 13.43f)
                reflectiveQuadTo(15.5f, 13f)
                reflectiveQuadToRelative(-1.07f, 0.42f)
                reflectiveQuadTo(14f, 14.5f)
                reflectiveQuadToRelative(0.43f, 1.07f)
                reflectiveQuadTo(15.5f, 16f)
                reflectiveQuadToRelative(1.07f, -0.43f)
                close()
                moveTo(7.5f, 17f)
                horizontalLineToRelative(9f)
                quadToRelative(0.65f, 0f, 1.07f, -0.43f)
                reflectiveQuadTo(18f, 15.5f)
                verticalLineTo(12f)
                horizontalLineTo(6f)
                verticalLineToRelative(3.5f)
                quadToRelative(0f, 0.65f, 0.43f, 1.07f)
                reflectiveQuadTo(7.5f, 17f)
                close()
                moveTo(12f, 5f)
                quadToRelative(2.8f, 0f, 4.04f, 0f)
                reflectiveQuadToRelative(1.61f, 0f)
                horizontalLineTo(6.45f)
                quadTo(6.9f, 5f, 8.13f, 5f)
                reflectiveQuadTo(12f, 5f)
                close()
            }
        }.build()
        return _train!!
    }

private var _train: ImageVector? = null
