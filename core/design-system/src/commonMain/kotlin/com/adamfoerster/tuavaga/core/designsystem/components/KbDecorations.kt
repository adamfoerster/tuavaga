package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/**
 * Diagonal stripes (CSS `repeating-linear-gradient`): [background] with bands of [stripe].
 * [period] is the horizontal distance between bands; each band's top edge is shifted right by
 * `height * slant` (stripes leaning "/").
 */
fun Modifier.diagonalStripes(stripe: Color, background: Color, period: Dp, slant: Float): Modifier =
    clipToBounds().drawBehind {
        drawRect(background)
        val p = period.toPx()
        val half = p / 2
        val shift = size.height * slant
        var x = -shift - p
        while (x < size.width + p) {
            val path = Path().apply {
                moveTo(x + shift, 0f)
                lineTo(x + shift + half, 0f)
                lineTo(x + half, size.height)
                lineTo(x, size.height)
                close()
            }
            drawPath(path, stripe)
            x += p
        }
    }

/** Apex/black hazard stripe at the top of the access screens (`x-zebra`). */
@Composable
fun KbZebraStripe(modifier: Modifier = Modifier) {
    val colors = KerbTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            // CSS -60deg, 20 px bands → 46 px horizontal period, tan(30°) slant.
            .diagonalStripes(colors.apex, colors.onApex, period = 46.dp, slant = 0.577f),
    )
}

/** Striped photo placeholder (`x-ph`) with a mono caption. Photos are not uploaded yet. */
@Composable
fun KbPhotoPlaceholder(
    caption: String,
    modifier: Modifier = Modifier,
) {
    val colors = KerbTheme.colors
    Box(
        modifier = modifier.diagonalStripes(colors.surfaceSunken, colors.surfaceRaised, period = 28.dp, slant = 1f),
        contentAlignment = Alignment.Center,
    ) {
        KbText(
            text = caption.uppercase(),
            style = KerbTheme.typography.dataSmall.copy(letterSpacing = 0.06.em),
            color = colors.inkMuted,
            modifier = Modifier.padding(8.dp),
        )
    }
}

/** 1 dp separator line (`x-hair`). */
@Composable
fun KbHairline(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(KerbTheme.colors.line))
}

/** Hairline — LABEL — hairline, like the "ou" between the login buttons. */
@Composable
fun KbLabeledDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KbHairline(Modifier.weight(1f))
        KbText(label, KerbTheme.typography.label, color = KerbTheme.colors.inkMuted)
        KbHairline(Modifier.weight(1f))
    }
}
