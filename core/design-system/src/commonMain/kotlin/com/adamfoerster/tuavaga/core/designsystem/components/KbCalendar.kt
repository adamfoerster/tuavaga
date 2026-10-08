package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** Look of a day in the month grid (CSS `x-day` variants). */
enum class KbDayState {
    /** Available. */
    Free,

    /** Not available (outside the weekly rule). */
    Closed,
    Past,
    Booked,
    Blocked,

    /** Selected for editing. */
    Selected,
}

@Immutable
data class KbDayCell(
    val day: Int,
    val state: KbDayState,
    /** Small mono mark under the number: "HOJE", "RES", "BLQ", "SEL". */
    val mark: String = "",
    val enabled: Boolean = true,
    /** Read by screen readers instead of the bare number, e.g. "12 de outubro, bloqueado". */
    val description: String? = null,
)

private val weekdayHeaders = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM")

/**
 * Month grid starting on Monday (boards 07, 15 and 25), inside a Kerb panel titled [title].
 * [leadingBlanks] empty cells come before day 1. [footer] holds the legend and notes.
 */
@Composable
fun KbMonthCalendar(
    title: String,
    leadingBlanks: Int,
    cells: List<KbDayCell>,
    onDayClick: (day: Int) -> Unit,
    modifier: Modifier = Modifier,
    previousLabel: String? = null,
    nextLabel: String? = null,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    KbPanel(title = title, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (previousLabel != null || nextLabel != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (previousLabel != null) MonthButton("← $previousLabel", "Mês anterior", onPrevious)
                    Spacer(Modifier.weight(1f))
                    if (nextLabel != null) MonthButton("$nextLabel →", "Próximo mês", onNext)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdayHeaders.forEach {
                    KbText(
                        text = it,
                        style = KerbTheme.typography.dataSmall,
                        color = KerbTheme.colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val slots: List<KbDayCell?> = List(leadingBlanks) { null } + cells
            slots.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { cell ->
                        if (cell == null) {
                            Spacer(Modifier.weight(1f).height(44.dp))
                        } else {
                            DayCell(cell, onClick = { onDayClick(cell.day) }, modifier = Modifier.weight(1f))
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(44.dp)) }
                }
            }
            footer()
        }
    }
}

@Composable
private fun MonthButton(text: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .background(KerbTheme.colors.surfaceSunken)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$description, $text" }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        KbText(text.uppercase(), KerbTheme.typography.dataSmall)
    }
}

/** Background, border and text colors of a day state; also used by the legend. */
@Composable
private fun Modifier.dayBackground(state: KbDayState): Modifier {
    val colors = KerbTheme.colors
    return when (state) {
        KbDayState.Free -> background(colors.surfaceSunken).border(1.dp, colors.line)
        KbDayState.Closed -> border(1.dp, colors.line)
        KbDayState.Past -> background(colors.surface)
        KbDayState.Booked -> background(colors.telemetry)
        KbDayState.Blocked -> diagonalStripes(colors.line, colors.surfaceSunken, period = 8.dp, slant = 1f)
        KbDayState.Selected -> background(colors.apex)
    }
}

@Composable
private fun dayInk(state: KbDayState): Color {
    val colors = KerbTheme.colors
    return when (state) {
        KbDayState.Free -> colors.ink
        KbDayState.Closed, KbDayState.Past, KbDayState.Blocked -> colors.inkMuted
        KbDayState.Booked -> colors.surface
        KbDayState.Selected -> colors.onApex
    }
}

@Composable
private fun DayCell(cell: KbDayCell, onClick: () -> Unit, modifier: Modifier) {
    val ink = dayInk(cell.state)
    Column(
        modifier = modifier
            .height(44.dp)
            .dayBackground(cell.state)
            .clickable(enabled = cell.enabled, role = Role.Checkbox, onClick = onClick)
            .semantics { cell.description?.let { contentDescription = it } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        KbText(cell.day.toString(), KerbTheme.typography.data.copy(fontSize = 14.sp, lineHeight = 16.sp), color = ink)
        KbText(cell.mark, KerbTheme.typography.dataSmall.copy(fontSize = 10.sp, lineHeight = 10.sp), color = ink)
    }
}

/** One legend entry: a 24 dp swatch of [state] and its label ("SEL · Em edição"). */
@Composable
fun KbDayLegend(state: KbDayState, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(24.dp).dayBackground(state))
        KbText(label.uppercase(), KerbTheme.typography.dataSmall, color = KerbTheme.colors.inkMuted)
    }
}
