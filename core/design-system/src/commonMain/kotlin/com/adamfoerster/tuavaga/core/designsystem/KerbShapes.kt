package com.adamfoerster.tuavaga.core.designsystem

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Kerb's signature corner: top-left and bottom-right cut at 45°, the other two square
 * (CSS `clip-path: polygon(cut 0, 100% 0, 100% calc(100% - cut), calc(100% - cut) 100%, 0 100%, 0 cut)`).
 */
class ChamferShape(private val cut: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { cut.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2)
        val path = Path().apply {
            moveTo(c, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(0f, size.height)
            lineTo(0f, c)
            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean = other is ChamferShape && other.cut == cut
    override fun hashCode(): Int = cut.hashCode()
}

object KerbShapes {
    val chamferSm = ChamferShape(6.dp)
    val chamferMd = ChamferShape(10.dp)
    val chamferLg = ChamferShape(18.dp)
}

/** Spacing scale (`--space-1` … `--space-8`). */
object KerbSpacing {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val s7 = 48.dp
    val s8 = 64.dp
}
