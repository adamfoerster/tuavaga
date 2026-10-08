package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** Draws a solid bar of [height] along the top edge (CSS `border-top` / `inset 0 3px` shadow). */
fun Modifier.topBar(color: Color, height: Dp): Modifier = drawWithContent {
    drawContent()
    drawRect(color, topLeft = Offset.Zero, size = Size(size.width, height.toPx()))
}

/** Small danger counter (notifications, new requests). Hidden when [count] is 0. */
@Composable
fun KbCountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val colors = KerbTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = 20.dp)
            .widthIn(min = 20.dp)
            .background(colors.danger)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        KbText(
            text = if (count > 99) "99+" else count.toString(),
            style = KerbTheme.typography.dataSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
            color = colors.onDanger,
        )
    }
}

/** Square 44 dp icon button on a raised surface (back, notifications), with an optional badge. */
@Composable
fun KbIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    enabled: Boolean = true,
) {
    val colors = KerbTheme.colors
    Box(modifier = modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.surfaceRaised)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            KbIcon(icon, contentDescription = null, tint = if (enabled) colors.ink else colors.inkMuted)
        }
        KbCountBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
    }
}

@Immutable
data class KbTab<T>(val value: T, val label: String, val icon: ImageVector, val badge: Int = 0)

/** Bottom tab bar (`Extensão · Tab bar`): the active tab gets ink text and an apex bar on top. */
@Composable
fun <T> KbTabBar(
    tabs: List<KbTab<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KerbTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .topBar(colors.line, 2.dp)
            .selectableGroup(),
    ) {
        tabs.forEach { tab ->
            val active = tab.value == selected
            val tint = if (active) colors.ink else colors.inkMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .then(if (active) Modifier.topBar(colors.apex, 3.dp) else Modifier)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab.value) })
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Box {
                    KbIcon(tab.icon, contentDescription = null, tint = tint)
                    KbCountBadge(tab.badge, Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp))
                }
                KbText(
                    text = tab.label.uppercase(),
                    style = KerbTheme.typography.sm.copy(fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.04.em),
                    color = tint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
