package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** Text in a Kerb style; display styles and labels are upper-cased like the design does. */
@Composable
fun KbText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = KerbTheme.colors.ink,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val shown = if (KerbTheme.typography.isUppercase(style)) text.uppercase() else text
    Text(
        text = shown,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** Form-level error message (danger, semibold). Renders nothing when [text] is null. */
@Composable
fun KbErrorText(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    KbText(
        text = text,
        style = KerbTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = KerbTheme.colors.danger,
        modifier = modifier,
    )
}

/** Neutral feedback message (e.g. "código reenviado"). Renders nothing when [text] is null. */
@Composable
fun KbInfoText(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    KbText(
        text = text,
        style = KerbTheme.typography.bodySmall,
        color = KerbTheme.colors.telemetry,
        modifier = modifier,
    )
}
