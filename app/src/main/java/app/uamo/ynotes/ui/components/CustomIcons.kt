package app.uamo.ynotes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {

    private fun mdiIcon(name: String, pathData: String): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.White)
        ).build()
    }

    // Material Design Icons (Pictogrammers MDI)
    val ShieldLockOutline: ImageVector by lazy {
        mdiIcon(
            "ShieldLockOutline",
            "M12,2L4,5V11.09C4,16.14 7.41,20.85 12,22C16.59,20.85 20,16.14 20,11.09V5L12,2M12,4.15L18,6.4V11.09C18,15.03 15.45,18.7 12,19.91C8.55,18.7 6,15.03 6,11.09V6.4L12,4.15M12,7A3,3 0 0,0 9,10C9,11.3 9.84,12.4 11,12.82V16H13V12.82C14.16,12.4 15,11.3 15,10A3,3 0 0,0 12,7M12,9A1,1 0 0,1 13,10A1,1 0 0,1 12,11A1,1 0 0,1 11,10A1,1 0 0,1 12,9Z"
        )
    }

    val NotebookEditOutline: ImageVector by lazy {
        mdiIcon(
            "NotebookEditOutline",
            "M21,11.11V5A2,2 0 0,0 19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H11.89C12.18,21.68 12.63,22.28 13.21,22.75L13.5,23H5A4,4 0 0,1 1,19V5A4,4 0 0,1 5,1H19A4,4 0 0,1 23,5V11.11L21,11.11M13.84,21.9L21.5,14.24C21.89,13.85 21.89,13.22 21.5,12.83L20.17,11.5C19.78,11.11 19.15,11.11 18.76,11.5L11.1,19.16C11,19.26 10.93,19.4 10.9,19.55L10.05,22.79C9.97,23.11 10.06,23.45 10.29,23.68C10.5,23.9 10.82,24 11.13,23.95L14.37,23.1C14.5,23.07 14.64,23 14.74,22.9L13.84,21.9M12.5,20.4L18.05,14.85L19.15,15.95L13.6,21.5L12.5,20.4Z"
        )
    }

    val WidgetsOutline: ImageVector by lazy {
        mdiIcon(
            "WidgetsOutline",
            "M16.66,1.15L9.34,8.46L10.75,9.88L16.66,3.97L22.58,9.88L24,8.46L16.66,1.15M3,6V12H9V6H3M5,8H7V10H5V8M9,15V21H3V15H9M7,19H5V17H7V19M15,15V21H21V15H15M19,19H17V17H19V19Z"
        )
    }

    val PaletteSwatchOutline: ImageVector by lazy {
        mdiIcon(
            "PaletteSwatchOutline",
            "M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22C13.1,22 14,21.1 14,20C14,19.5 13.8,19 13.4,18.6C13,18.2 12.8,17.7 12.8,17.2C12.8,16.1 13.7,15.2 14.8,15.2H16C19.3,15.2 22,12.5 22,9.2C22,5.2 17.5,2 12,2M12,4C16.4,4 20,6.6 20,9.2C20,11.4 18.2,13.2 16,13.2H14.8C12.6,13.2 10.8,15 10.8,17.2C10.8,17.7 11,18.2 11.4,18.6C11.8,19 12,19.5 12,20C12,20 12,20 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4M6.5,10A1.5,1.5 0 0,0 5,11.5A1.5,1.5 0 0,0 6.5,13A1.5,1.5 0 0,0 8,11.5A1.5,1.5 0 0,0 6.5,10M9.5,6A1.5,1.5 0 0,0 8,7.5A1.5,1.5 0 0,0 9.5,9A1.5,1.5 0 0,0 11,7.5A1.5,1.5 0 0,0 9.5,6M14.5,6A1.5,1.5 0 0,0 13,7.5A1.5,1.5 0 0,0 14.5,9A1.5,1.5 0 0,0 16,7.5A1.5,1.5 0 0,0 14.5,6M17.5,10A1.5,1.5 0 0,0 16,11.5A1.5,1.5 0 0,0 17.5,13A1.5,1.5 0 0,0 19,11.5A1.5,1.5 0 0,0 17.5,10Z"
        )
    }

    val LightningBolt: ImageVector by lazy {
        mdiIcon(
            "LightningBolt",
            "M11,15H6L13,1V9H18L11,23V15Z"
        )
    }

    val FormatListCheckbox: ImageVector by lazy {
        mdiIcon(
            "FormatListCheckbox",
            "M3,5H9V11H3V5M5,7V9H7V7H5M3,13H9V19H3V13M5,15V17H7V15H5M11,7H21V9H11V7M11,15H21V17H11V15Z"
        )
    }

    val LockCheckOutline: ImageVector by lazy {
        mdiIcon(
            "LockCheckOutline",
            "M12,17C10.89,17 10,16.1 10,15C10,13.89 10.89,13 12,13A2,2 0 0,1 14,15A2,2 0 0,1 12,17M18,20V10H6V20H18M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.89,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"
        )
    }

    val GooglePlay: ImageVector
        get() = ImageVector.Builder(
            name = "GooglePlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(22.018f, 13.298f)
                lineToRelative(-3.919f, 2.218f)
                lineToRelative(-3.515f, -3.493f)
                lineToRelative(3.543f, -3.521f)
                lineToRelative(3.891f, 2.202f)
                arcToRelative(1.49f, 1.49f, 0f, false, true, 0f, 2.594f)
                close()
                moveTo(1.337f, 0.924f)
                arcToRelative(1.486f, 1.486f, 0f, false, false, -0.112f, 0.568f)
                verticalLineToRelative(21.017f)
                curveToRelative(0f, 0.217f, 0.045f, 0.419f, 0.124f, 0.6f)
                lineToRelative(11.155f, -11.087f)
                lineTo(1.337f, 0.924f)
                close()
                moveTo(13.544f, 10.989f)
                lineToRelative(3.258f, -3.238f)
                lineTo(3.45f, 0.195f)
                arcToRelative(1.466f, 1.466f, 0f, false, false, -0.946f, -0.179f)
                lineToRelative(11.04f, 10.973f)
                close()
                moveTo(13.544f, 13.056f)
                lineToRelative(-11f, 10.933f)
                curveToRelative(0.298f, 0.036f, 0.612f, -0.016f, 0.906f, -0.183f)
                lineToRelative(13.324f, -7.54f)
                lineToRelative(-3.23f, -3.21f)
                close()
            }
        }.build()

    val Binance: ImageVector
        get() = ImageVector.Builder(
            name = "Binance",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16.624f, 13.9202f)
                lineToRelative(2.7175f, 2.7154f)
                lineToRelative(-7.353f, 7.353f)
                lineToRelative(-7.353f, -7.352f)
                lineToRelative(2.7175f, -2.7164f)
                lineToRelative(4.6355f, 4.6595f)
                lineToRelative(4.6356f, -4.6595f)
                close()
                moveToRelative(4.6366f, -4.6366f)
                lineTo(24f, 12f)
                lineToRelative(-2.7154f, 2.7164f)
                lineTo(18.5682f, 12f)
                lineToRelative(2.6924f, -2.7164f)
                close()
                moveToRelative(-9.272f, 0.001f)
                lineToRelative(2.7163f, 2.6914f)
                lineToRelative(-2.7164f, 2.7174f)
                verticalLineToRelative(-0.001f)
                lineTo(9.2721f, 12f)
                lineToRelative(2.7164f, -2.7154f)
                close()
                moveToRelative(-9.2722f, -0.001f)
                lineTo(5.4088f, 12f)
                lineToRelative(-2.6914f, 2.6924f)
                lineTo(0f, 12f)
                lineToRelative(2.7164f, -2.7164f)
                close()
                moveTo(11.9885f, 0.0115f)
                lineToRelative(7.353f, 7.329f)
                lineToRelative(-2.7174f, 2.7154f)
                lineToRelative(-4.6356f, -4.6356f)
                lineToRelative(-4.6355f, 4.6595f)
                lineToRelative(-2.7174f, -2.7154f)
                lineToRelative(7.353f, -7.353f)
                close()
            }
        }.build()

    val PayPal: ImageVector
        get() = ImageVector.Builder(
            name = "PayPal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(15.607f, 4.653f)
                horizontalLineTo(8.941f)
                lineTo(6.645f, 19.251f)
                horizontalLineTo(1.82f)
                lineTo(4.862f, 0f)
                horizontalLineToRelative(7.995f)
                curveToRelative(3.754f, 0f, 6.375f, 2.294f, 6.473f, 5.513f)
                curveToRelative(-0.648f, -0.478f, -2.105f, -0.86f, -3.722f, -0.86f)
                moveToRelative(6.57f, 5.546f)
                curveToRelative(0f, 3.41f, -3.01f, 6.853f, -6.958f, 6.853f)
                horizontalLineToRelative(-2.493f)
                lineTo(11.595f, 24f)
                horizontalLineTo(6.74f)
                lineToRelative(1.845f, -11.538f)
                horizontalLineToRelative(3.592f)
                curveToRelative(4.208f, 0f, 7.346f, -3.634f, 7.153f, -6.949f)
                arcToRelative(5.24f, 5.24f, 0f, false, true, 2.848f, 4.686f)
                moveTo(9.653f, 5.546f)
                horizontalLineToRelative(6.408f)
                curveToRelative(0.907f, 0f, 1.942f, 0.222f, 2.363f, 0.541f)
                curveToRelative(-0.195f, 2.741f, -2.655f, 5.483f, -6.441f, 5.483f)
                horizontalLineTo(8.714f)
                close()
            }
        }.build()
}
