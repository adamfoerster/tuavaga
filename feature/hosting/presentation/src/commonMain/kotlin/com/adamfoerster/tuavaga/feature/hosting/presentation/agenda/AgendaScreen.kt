package com.adamfoerster.tuavaga.feature.hosting.presentation.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayLegend
import com.adamfoerster.tuavaga.core.designsystem.components.KbDayState
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbHairline
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbListItem
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbMonthCalendar
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.presentation.counterpartShortName
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.leadingBlanks
import com.adamfoerster.tuavaga.core.presentation.monthShort
import com.adamfoerster.tuavaga.core.presentation.monthTitle
import com.adamfoerster.tuavaga.core.presentation.shortPt
import com.adamfoerster.tuavaga.core.presentation.statusTag
import com.adamfoerster.tuavaga.core.presentation.unitsLabel
import com.adamfoerster.tuavaga.feature.hosting.domain.SpotStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AgendaRoot(
    spotId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRequests: () -> Unit,
    onOpenBooking: (bookingId: String) -> Unit,
    viewModel: AgendaViewModel = koinViewModel { parametersOf(spotId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AgendaScreen(
        state = state,
        onAction = { action ->
            when (action) {
                AgendaAction.OnBackClick -> onBack()
                AgendaAction.OnEditClick -> onEdit()
                AgendaAction.OnRequestsClick -> onRequests()
                is AgendaAction.OnBookingClick -> onOpenBooking(action.bookingId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** Board 28 · Agenda da vaga. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgendaScreen(
    state: AgendaState,
    onAction: (AgendaAction) -> Unit,
) {
    val typography = KerbTheme.typography
    val spot = state.spot
    KbScreen(
        header = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KbIconButton(KbIcons.Back, "Voltar", { onAction(AgendaAction.OnBackClick) })
            }
        },
        bottomBar = if (spot == null) null else ({
            KbButton("Editar disponibilidade", { onAction(AgendaAction.OnEditClick) }, Modifier.fillMaxWidth())
            KbButton(
                "Ver solicitações",
                { onAction(AgendaAction.OnRequestsClick) },
                Modifier.fillMaxWidth(),
                variant = KbButtonVariant.Ghost,
            )
        }),
    ) {
        when {
            spot == null && state.isLoading -> KbMeter(value = 0.4f, label = "Carregando a agenda", segments = 24, redline = 1f)
            spot == null -> {
                KbErrorText(state.error?.asString())
                KbButton("Tentar de novo", { onAction(AgendaAction.OnRetry) }, Modifier.fillMaxWidth())
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val active = spot.status == SpotStatus.ACTIVE
                    KbTag(if (active) "Ativa" else "Pausada", tone = if (active) KbTone.Go else KbTone.Caution)
                    KbText("Agenda", typography.lg)
                    KbText("Vaga ${spot.code}".uppercase(), typography.dataSmall, color = KerbTheme.colors.inkMuted)
                }
                KbMonthCalendar(
                    title = state.month.monthTitle(),
                    leadingBlanks = state.month.leadingBlanks(),
                    cells = state.cells(),
                    onDayClick = {},
                    previousLabel = if (state.canGoBack) state.month.plus(-1, DateTimeUnit.MONTH).monthShort() else null,
                    nextLabel = state.month.plus(1, DateTimeUnit.MONTH).monthShort(),
                    onPrevious = { onAction(AgendaAction.OnPreviousMonth) },
                    onNext = { onAction(AgendaAction.OnNextMonth) },
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KbDayLegend(KbDayState.Free, "Livre")
                        KbDayLegend(KbDayState.Booked, "RES · Reservada")
                        KbDayLegend(KbDayState.Blocked, "BLQ · Bloqueada")
                    }
                }
                val bookings = state.monthBookings
                KbPanel(title = "Reservas do mês", aside = "${bookings.size}") {
                    if (bookings.isEmpty()) {
                        KbText("Nenhuma reserva neste mês.", typography.body, color = KerbTheme.colors.inkMuted)
                    }
                    Column {
                        bookings.forEachIndexed { index, booking ->
                            if (index > 0) KbHairline()
                            AgendaRow(booking, onClick = { onAction(AgendaAction.OnBookingClick(booking.id)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaRow(booking: Booking, onClick: () -> Unit) {
    val (status, tone) = booking.statusTag()
    KbListItem(
        title = booking.counterpartShortName,
        meta = agendaPeriod(booking),
        onClick = onClick,
        trailing = { KbTag(status, tone = tone) },
    )
}

/** "QUA 07 · 08:00 → 18:00" or "SÁB 10 → DOM 11 · 2 DIÁRIAS" (board 28). */
private fun agendaPeriod(booking: Booking): String {
    fun LocalDateTime.day() = "${date.dayOfWeek.shortPt()} ${date.day.toString().padStart(2, '0')}"
    val period = booking.period
    return if (period.start.date == period.end.date) {
        "${period.start.day()} · ${period.start.hhmm()} → ${period.end.hhmm()}"
    } else {
        val tail = if (booking.quote.unit == BillingUnit.HOUR) "${period.start.hhmm()} → ${period.end.hhmm()}" else booking.quote.unitsLabel()
        "${period.start.day()} → ${period.end.day()} · $tail"
    }.uppercase()
}
