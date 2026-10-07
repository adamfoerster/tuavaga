package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adamfoerster.tuavaga.core.designsystem.KerbColors
import com.adamfoerster.tuavaga.core.designsystem.KerbShapes
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** Semantic tone of a tag. The design always pairs a tone with a word, never color alone. */
enum class KbTone { Neutral, Go, Info, Caution, Danger }

/** Background and content colors of a [KbTone] (CSS `kb-tag--*`). */
fun KbTone.tagColors(colors: KerbColors): Pair<Color, Color> = when (this) {
    KbTone.Neutral -> colors.surfaceSunken to colors.ink
    KbTone.Go -> colors.volt to colors.onVolt
    KbTone.Info -> colors.telemetry to colors.surface
    KbTone.Caution -> colors.caution to colors.onCaution
    KbTone.Danger -> colors.danger to colors.onDanger
}

@Composable
fun KbTag(
    text: String,
    modifier: Modifier = Modifier,
    tone: KbTone = KbTone.Neutral,
) {
    val (background, content) = tone.tagColors(KerbTheme.colors)
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(KerbShapes.chamferSm)
            .background(background)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        KbText(
            text = text.uppercase(),
            style = KerbTheme.typography.sm.copy(fontSize = 13.sp, lineHeight = 13.sp, letterSpacing = 0.1.em),
            color = content,
            maxLines = 1,
        )
    }
}
