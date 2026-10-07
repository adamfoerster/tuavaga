package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adamfoerster.tuavaga.core.designsystem.KerbShapes
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/**
 * Kerb toolbar (`kb-toolbar`) used as a segmented control: exactly one option is active.
 * With [fill] (the design's `x-fill`) it takes the full width and splits it evenly.
 */
@Composable
fun <T> KbToolbar(
    options: List<KbOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = KerbTheme.colors
    Row(
        modifier = modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(KerbShapes.chamferMd)
            .background(colors.surfaceRaised)
            .bottomBar(colors.apex, 3.dp)
            .padding(start = 10.dp, top = 4.dp, end = 4.dp, bottom = 7.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val active = option.value == selected
            Box(
                modifier = Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .height(36.dp)
                    .background(if (active) colors.apex else Color.Transparent)
                    .selectable(
                        selected = active,
                        enabled = enabled,
                        role = Role.Tab,
                        onClick = { onSelect(option.value) },
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                KbText(
                    text = option.label.uppercase(),
                    style = KerbTheme.typography.sm.copy(fontSize = 15.sp, lineHeight = 15.sp, letterSpacing = 0.08.em),
                    color = when {
                        active -> colors.onApex
                        enabled -> colors.ink
                        else -> colors.inkMuted
                    },
                    maxLines = 1,
                )
            }
        }
    }
}
