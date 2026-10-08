package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/**
 * Selectable row with a colored left edge, used for condominium results and the condominium
 * switcher: title, optional [trailing] tag, mono [meta] line and a muted [text] line.
 * Selected rows get an apex edge (and the apex glow outline when [glow]).
 */
@Composable
fun KbListItem(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    text: String? = null,
    selected: Boolean = false,
    glow: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (selected && glow) Modifier.border(1.dp, colors.apex) else Modifier)
            .background(if (selected) colors.surfaceSunken else colors.surfaceRaised)
            .leftBar(if (selected) colors.apex else colors.line, 4.dp)
            .then(
                if (onClick != null) {
                    Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KbText(title, typography.sm, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            trailing?.invoke(this)
        }
        if (meta != null) KbText(meta.uppercase(), typography.dataSmall, color = colors.inkMuted)
        if (text != null) KbText(text, typography.bodySmall, color = colors.inkMuted)
    }
}

/** Section title (`t-md`) with a hairline under it, e.g. "Veículo", "Garagem". */
@Composable
fun KbSectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KbText(title, KerbTheme.typography.md)
        KbHairline()
    }
}
