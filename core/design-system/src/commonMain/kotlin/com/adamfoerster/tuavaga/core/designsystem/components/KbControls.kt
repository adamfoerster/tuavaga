package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adamfoerster.tuavaga.core.designsystem.KerbShapes
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** Filter chip (`x-chip`): outlined when off, apex when [selected]. */
@Composable
fun KbChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KerbTheme.colors
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(KerbShapes.chamferSm)
            .background(if (selected) colors.apex else colors.surfaceSunken)
            .then(if (selected) Modifier else Modifier.border(2.dp, colors.lineStrong, KerbShapes.chamferSm))
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        KbText(
            text = text.uppercase(),
            style = KerbTheme.typography.sm.copy(fontSize = 15.sp, lineHeight = 15.sp, letterSpacing = 0.08.em),
            color = if (selected) colors.onApex else colors.ink,
            maxLines = 1,
        )
    }
}

/** Initials avatar (`x-avatar`), e.g. "MR". */
@Composable
fun KbAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(KerbShapes.chamferSm)
            .background(KerbTheme.colors.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        KbText(
            text = initials.uppercase(),
            style = KerbTheme.typography.sm.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 20 / 48).sp,
                lineHeight = (size.value * 20 / 48).sp,
            ),
        )
    }
}

/** Two-letter initials of a name: "Marina Ribeiro" → "MR", "Adam" → "AD". */
fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2)
        else -> "${parts.first().first()}${parts.last().first()}"
    }.uppercase()
}

/** Checkbox row (`x-check` + text), the whole row toggles. */
@Composable
fun KbCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = KerbTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (checked) colors.apex else colors.surfaceSunken, RoundedCornerShape(2.dp))
                .then(if (checked) Modifier else Modifier.border(2.dp, colors.lineStrong, RoundedCornerShape(2.dp))),
        ) {
            if (checked) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val stroke = 2.5.dp.toPx()
                    val w = size.width
                    drawLine(colors.onApex, Offset(w * 0.22f, w * 0.52f), Offset(w * 0.42f, w * 0.72f), stroke, StrokeCap.Square)
                    drawLine(colors.onApex, Offset(w * 0.42f, w * 0.72f), Offset(w * 0.78f, w * 0.30f), stroke, StrokeCap.Square)
                }
            }
        }
        KbText(text, KerbTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}
