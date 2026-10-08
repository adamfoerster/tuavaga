package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** An option of a [KbSelect] or [KbToolbar]; [hint] is the mono text on the right of a menu item. */
@Immutable
data class KbOption<T>(val value: T, val label: String, val hint: String? = null)

/** Kerb select (`kb-select`): field-like button that opens a Kerb menu. */
@Composable
fun <T> KbSelect(
    label: String,
    options: List<KbOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Selecione",
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    var expanded by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val current = options.firstOrNull { it.value == selected }
    val shape = RoundedCornerShape(2.dp)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KbFieldLabel(label)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { anchorWidth = it.width }
                    .focusRing(expanded, colors.telemetry)
                    .height(44.dp)
                    .background(colors.surfaceSunken, shape)
                    .border(
                        width = 2.dp,
                        color = when {
                            error != null -> colors.danger
                            expanded -> colors.telemetry
                            else -> colors.lineStrong
                        },
                        shape = shape,
                    )
                    .clickable(enabled = enabled, role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KbText(
                    text = current?.label ?: placeholder,
                    style = typography.data,
                    color = if (current == null || !enabled) colors.inkMuted else colors.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                KbText("▼", typography.dataSmall, color = colors.inkMuted)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                // Same width as the field, like the design's kb-menu under a select.
                modifier = Modifier.width(with(density) { anchorWidth.toDp() }),
                shape = RoundedCornerShape(4.dp),
                containerColor = colors.surfaceRaised,
                border = BorderStroke(2.dp, colors.lineStrong),
            ) {
                options.forEach { option ->
                    KbMenuItem(
                        label = option.label,
                        hint = option.hint,
                        selected = option.value == selected,
                        onClick = {
                            expanded = false
                            onSelect(option.value)
                        },
                    )
                }
            }
        }
        KbFieldMessage(error, hint)
    }
}

@Composable
private fun KbMenuItem(label: String, hint: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = KerbTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(if (selected) colors.surfaceSunken else Color.Transparent)
            .leftBar(if (selected) colors.apex else Color.Transparent, 3.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KbText(
            text = label,
            style = KerbTheme.typography.body.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (hint != null) {
            KbText(hint, KerbTheme.typography.dataSmall, color = if (selected) colors.apexText else colors.inkMuted)
        }
    }
}
