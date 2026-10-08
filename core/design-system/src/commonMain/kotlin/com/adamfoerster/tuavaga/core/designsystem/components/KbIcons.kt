package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/**
 * Stroke icons of the design (`x-icon`: 24×24, 2 px, square caps, miter joins), from its SVG paths.
 * Draw them with [KbIcon], which tints them.
 */
object KbIcons {
    val Search = icon("Search", "M16 10a6 6 0 1 1-12 0a6 6 0 1 1 12 0z", "M15 15l6 6")
    val Calendar = icon("Calendar", "M3 5h18v16H3z", "M3 10h18M8 3v4M16 3v4")
    val Spot = icon("Spot", "M3 3h18v18H3z", "M9 18V6h6v7H9")
    val Message = icon("Message", "M3 4h18v12H9l-5 4v-4H3z")
    val Profile = icon("Profile", "M8 3h8v9H8z", "M4 21v-4h16v4")
    val Bell = icon("Bell", "M6 17V10a6 6 0 0 1 12 0v7M4 17h16M10 21h4")
    val Back = icon("Back", "M20 12H5M11 5l-7 7 7 7")
    val Plus = icon("Plus", "M12 5v14M5 12h14")
    val Close = icon("Close", "M6 6l12 12M18 6L6 18")
    val ArrowUp = icon("ArrowUp", "M12 19V5M5 12l7-7 7 7")

    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Square,
                    strokeLineJoin = StrokeJoin.Miter,
                )
            }
        }.build()
}

@Composable
fun KbIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = KerbTheme.colors.ink,
    size: Dp = 24.dp,
) {
    Image(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
