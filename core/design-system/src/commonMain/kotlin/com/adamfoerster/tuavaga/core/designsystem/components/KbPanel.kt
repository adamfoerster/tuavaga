package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbShapes
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** Draws a solid bar of [height] along the bottom edge, over the content (CSS `border-bottom`/`::after`). */
fun Modifier.bottomBar(color: Color, height: Dp): Modifier = drawWithContent {
    drawContent()
    val h = height.toPx()
    drawRect(color, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
}

/** Draws a solid bar of [width] along the left edge (CSS `border-left`). */
fun Modifier.leftBar(color: Color, width: Dp): Modifier = drawWithContent {
    drawContent()
    drawRect(color, size = Size(width.toPx(), size.height))
}

/** Kerb panel (`kb-panel`): raised, chamfered block with an optional head (title + aside). */
@Composable
fun KbPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    aside: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KerbTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(KerbShapes.chamferLg)
            .background(colors.surfaceRaised),
    ) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSunken)
                    .bottomBar(colors.apex, 3.dp)
                    .padding(start = 26.dp, top = 12.dp, end = 16.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                KbText(title, KerbTheme.typography.sm, modifier = Modifier.weight(1f))
                if (aside != null) {
                    KbText(aside, KerbTheme.typography.dataSmall, color = colors.inkMuted)
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

/**
 * Kerb card (`kb-card`): chamfered block with a 4 dp bottom line (apex when [selected]).
 * The body gets 16 dp padding and 8 dp gaps; [footer] buttons are aligned to the end.
 */
@Composable
fun KbCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KerbTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(KerbShapes.chamferLg)
            .background(if (selected) colors.surfaceSunken else colors.surfaceRaised)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .bottomBar(if (selected) colors.apex else colors.line, 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        if (footer != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = footer,
            )
        }
    }
}
