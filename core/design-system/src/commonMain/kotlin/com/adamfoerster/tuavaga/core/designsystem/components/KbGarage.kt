package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** State of a spot on the garage map (CSS `x-spot` variants). */
enum class KbSpotState { Free, Busy, Selected, Mine }

@Composable
private fun Modifier.spotLook(state: KbSpotState): Modifier {
    val colors = KerbTheme.colors
    return when (state) {
        KbSpotState.Free -> background(colors.surfaceSunken).border(2.dp, colors.voltText)
        KbSpotState.Busy -> diagonalStripes(colors.line, colors.surfaceSunken, period = 8.dp, slant = 1f)
        KbSpotState.Selected -> background(colors.apex).border(1.dp, colors.apex)
        KbSpotState.Mine -> background(colors.surfaceSunken).border(2.dp, colors.telemetry)
    }
}

/** One spot of the garage map (46 × 60), labelled with its number. */
@Composable
fun KbGarageSpot(
    label: String,
    state: KbSpotState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = KerbTheme.colors
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 60.dp)
            .spotLook(state)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { description?.let { contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        KbText(
            text = label,
            style = KerbTheme.typography.dataSmall,
            color = when (state) {
                KbSpotState.Busy -> colors.inkMuted
                KbSpotState.Selected -> colors.onApex
                else -> colors.ink
            },
        )
    }
}

/** Legend entry of the garage map: a small swatch and its label. */
@Composable
fun KbSpotLegend(state: KbSpotState, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(width = 16.dp, height = 20.dp).spotLook(state))
        Column { KbText(label.uppercase(), KerbTheme.typography.dataSmall, color = KerbTheme.colors.inkMuted) }
    }
}
