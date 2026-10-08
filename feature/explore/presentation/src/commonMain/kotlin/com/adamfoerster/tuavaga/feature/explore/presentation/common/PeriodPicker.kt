package com.adamfoerster.tuavaga.feature.explore.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbBottomSheet
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbSelect
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.domain.spot.SpotFormats
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.short
import com.adamfoerster.tuavaga.core.presentation.weekdayDayMonth
import com.adamfoerster.tuavaga.feature.explore.domain.BookingPeriod
import com.adamfoerster.tuavaga.feature.explore.domain.plusMinutes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/** How far ahead a booking can start. */
const val BOOKING_DAYS_AHEAD = 60

/** Problems with a period chosen in the sheet; `null` when it can be searched. */
fun validatePeriod(period: BookingPeriod, now: LocalDateTime): UiText? = when {
    !period.isValid -> UiText.Dynamic("A saída precisa ser depois da entrada.")
    period.start < now.plusMinutes(-15) -> UiText.Dynamic("A entrada já passou. Escolha um horário a partir de agora.")
    period.start.date > now.date.plus(BOOKING_DAYS_AHEAD, DateTimeUnit.DAY) ->
        UiText.Dynamic("Dá para reservar até $BOOKING_DAYS_AHEAD dias à frente.")
    else -> null
}

/** Entrada / Saída shown like fields (board 04); tapping opens [PeriodSheet]. */
@Composable
fun PeriodFields(period: BookingPeriod, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PeriodField("Entrada", period.start.short(), onClick, Modifier.weight(1f))
        PeriodField("Saída", period.end.short(), onClick, Modifier.weight(1f))
    }
}

@Composable
private fun PeriodField(label: String, value: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = KerbTheme.colors
    val shape = RoundedCornerShape(2.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KbText(label, KerbTheme.typography.label, color = colors.inkMuted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(colors.surfaceSunken, shape)
                .border(2.dp, colors.lineStrong, shape)
                .clickable(role = Role.Button, onClickLabel = "Mudar período", onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KbText(value, KerbTheme.typography.data, maxLines = 1)
        }
    }
}

private val timeOptions = (0 until 48).map { half -> KbOption(half * 30, SpotFormats.formatTime(half * 30)) }

/** Sheet to choose Entrada and Saída: a date and a half-hour time for each. */
@Composable
fun PeriodSheet(
    initial: BookingPeriod,
    now: LocalDateTime,
    onApply: (BookingPeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    var startDate by remember { mutableStateOf(initial.start.date) }
    var startTime by remember { mutableStateOf(initial.start.hour * 60 + initial.start.minute.roundDownTo30()) }
    var endDate by remember { mutableStateOf(initial.end.date) }
    var endTime by remember { mutableStateOf(initial.end.hour * 60 + initial.end.minute.roundDownTo30()) }
    var error by remember { mutableStateOf<UiText?>(null) }

    val dates = remember(now.date) {
        (0..BOOKING_DAYS_AHEAD).map { offset ->
            val date = now.date.plus(offset, DateTimeUnit.DAY)
            KbOption(date, if (offset == 0) "Hoje · ${date.weekdayDayMonth()}" else date.weekdayDayMonth())
        }
    }

    KbBottomSheet(onDismiss = onDismiss) {
        KbText("Período", KerbTheme.typography.md)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbSelect("Entrada", dates, startDate, { startDate = it; error = null }, Modifier.weight(3f))
            KbSelect("Hora", timeOptions, startTime, { startTime = it; error = null }, Modifier.weight(2f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbSelect("Saída", dates, endDate, { endDate = it; error = null }, Modifier.weight(3f))
            KbSelect("Hora", timeOptions, endTime, { endTime = it; error = null }, Modifier.weight(2f))
        }
        KbErrorText(error?.asString())
        KbButton(
            text = "Aplicar",
            onClick = {
                val period = BookingPeriod(startDate.at(startTime), endDate.at(endTime))
                val problem = validatePeriod(period, now)
                if (problem == null) onApply(period) else error = problem
            },
            size = KbButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun Int.roundDownTo30(): Int = this / 30 * 30

private fun LocalDate.at(minutes: Int): LocalDateTime = LocalDateTime(this, LocalTime(minutes / 60, minutes % 60))
