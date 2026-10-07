package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import kotlin.math.roundToInt

/** Number of lit segments for [value] in a meter of [segments] (same rounding as the design). */
fun meterLitSegments(value: Float, segments: Int): Int =
    (value.coerceIn(0f, 1f) * segments).roundToInt()

/**
 * Kerb meter (`kb-meter`): a row of skewed segments. Used for wizard progress
 * ("Passo 1 de 3" with `segments = 3`) and for time readouts. Lit segments past [redline] turn danger.
 */
@Composable
fun KbMeter(
    value: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    readout: String? = null,
    segments: Int = 24,
    redline: Float = 0.8f,
) {
    val colors = KerbTheme.colors
    val lit = meterLitSegments(value, segments)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
                if (label != null) contentDescription = listOfNotNull(label, readout).joinToString(" · ")
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label != null) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                KbText(label, KerbTheme.typography.label, color = colors.inkMuted, modifier = Modifier.weight(1f))
                if (readout != null) KbText(readout, KerbTheme.typography.dataSmall)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(colors.surfaceSunken, RoundedCornerShape(2.dp))
                .padding(3.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gap = 3.dp.toPx()
                val w = (size.width - gap * (segments - 1)) / segments
                val h = size.height
                val skew = h * 0.364f // tan(20°): CSS skewX(-20deg)
                repeat(segments) { i ->
                    val x = i * (w + gap)
                    val on = i < lit
                    val hot = (i + 1).toFloat() / segments > redline
                    val color = when {
                        on && hot -> colors.danger
                        on -> colors.telemetry
                        else -> colors.lineStrong.copy(alpha = 0.35f)
                    }
                    val path = Path().apply {
                        moveTo(x + skew / 2, 0f)
                        lineTo(x + w + skew / 2, 0f)
                        lineTo(x + w - skew / 2, h)
                        lineTo(x - skew / 2, h)
                        close()
                    }
                    drawPath(path, color)
                }
            }
        }
    }
}
