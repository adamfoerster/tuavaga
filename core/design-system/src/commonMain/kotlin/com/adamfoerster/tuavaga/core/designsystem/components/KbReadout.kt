package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/**
 * Kerb readout (`kb-readout`): label, big mono value with a small [unit], and an optional [delta]
 * line colored by [deltaTone] (Go / Danger / otherwise telemetry). [tone] colors the value itself
 * (board 24 "Entrada 08:02" in go, board 18 "Passou do horário" in danger); the bar follows it.
 */
@Composable
fun KbReadout(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    delta: String? = null,
    deltaTone: KbTone = KbTone.Info,
    tone: KbTone? = null,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    val valueColor = when (tone) {
        KbTone.Go -> colors.voltText
        KbTone.Danger -> colors.danger
        KbTone.Caution -> colors.cautionText
        else -> colors.ink
    }
    Column(
        modifier = modifier
            .background(colors.surfaceRaised)
            .leftBar(if (tone == KbTone.Danger) colors.danger else colors.apex, 4.dp)
            .padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
    ) {
        KbText(label, typography.label, color = colors.inkMuted, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KbText(value, typography.dataXl, color = valueColor, modifier = Modifier.alignByBaseline(), maxLines = 1)
            if (unit != null) {
                KbText(unit, typography.dataSmall, color = colors.inkMuted, modifier = Modifier.alignByBaseline())
            }
        }
        if (delta != null) {
            KbText(
                text = delta,
                style = typography.dataSmall,
                color = when (deltaTone) {
                    KbTone.Go -> colors.voltText
                    KbTone.Danger -> colors.danger
                    else -> colors.telemetry
                },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
