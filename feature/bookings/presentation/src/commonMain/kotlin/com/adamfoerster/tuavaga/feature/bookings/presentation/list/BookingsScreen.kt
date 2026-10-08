package com.adamfoerster.tuavaga.feature.bookings.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbCard
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.KbToolbar
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.BookingTab
import com.adamfoerster.tuavaga.core.presentation.counterpartShortName
import com.adamfoerster.tuavaga.core.presentation.formatMoney
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.rangeLabel
import com.adamfoerster.tuavaga.core.presentation.statusTag
import org.koin.compose.viewmodel.koinViewModel

/** Content of the Reservas tab (inside the app shell). */
@Composable
fun BookingsRoot(
    onOpenBooking: (bookingId: String) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BookingsScreen(
        state = state,
        modifier = modifier,
        onAction = { action ->
            when (action) {
                is BookingsAction.OnBookingClick -> onOpenBooking(action.bookingId)
                BookingsAction.OnExploreClick -> onExplore()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

private val tabLabels = mapOf(
    BookingTab.UPCOMING to ("Próximas" to "PRÓXIMAS"),
    BookingTab.ONGOING to ("Em curso" to "EM CURSO"),
    BookingTab.HISTORY to ("Histórico" to "NO HISTÓRICO"),
)

/** Board 12 · Reservas (tabs Próximas / Em curso / Histórico). */
@Composable
fun BookingsScreen(
    state: BookingsState,
    onAction: (BookingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KbText("Reservas", typography.lg)
        KbToolbar(
            options = BookingTab.entries.map { tab ->
                val count = state.count(tab)
                KbOption(tab, if (count > 0 && tab != BookingTab.HISTORY) "${tabLabels.getValue(tab).first} $count" else tabLabels.getValue(tab).first)
            },
            selected = state.tab,
            onSelect = { onAction(BookingsAction.OnTabSelect(it)) },
        )
        if (state.error != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KbTag("Sem conexão", tone = KbTone.Danger)
                KbText(
                    text = "Mostrando as reservas salvas.",
                    style = typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f),
                )
                KbButton("Atualizar", { onAction(BookingsAction.OnRefresh) }, variant = KbButtonVariant.Ghost, size = KbButtonSize.Small)
            }
        }
        val visible = state.visible
        when {
            visible.isEmpty() && state.isRefreshing ->
                KbMeter(value = 0.4f, label = "Carregando reservas", segments = 24, redline = 1f)
            visible.isEmpty() -> EmptyPanel(state.tab, onAction)
            else -> {
                KbText(
                    text = buildString {
                        append("${visible.size} ${tabLabels.getValue(state.tab).second}")
                        val condos = state.condoCount
                        append(" · $condos ${if (condos == 1) "CONDOMÍNIO" else "CONDOMÍNIOS"}")
                    },
                    style = typography.dataSmall,
                    color = colors.inkMuted,
                )
                visible.forEach { booking -> BookingCard(booking, onClick = { onAction(BookingsAction.OnBookingClick(booking.id)) }) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookingCard(booking: Booking, onClick: () -> Unit) {
    val typography = KerbTheme.typography
    val (status, tone) = booking.statusTag()
    KbCard(
        onClick = onClick,
        footer = {
            KbText(
                text = cardFooter(booking),
                style = typography.bodySmall,
                color = KerbTheme.colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
            KbButton("Abrir", onClick, variant = KbButtonVariant.Ghost, size = KbButtonSize.Small)
        },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KbTag(status, tone = tone)
            KbTag(booking.condoName, tone = KbTone.Info)
        }
        KbText("Vaga ${booking.spotCode}", typography.md)
        KbText(booking.period.rangeLabel().let {
            if (booking.period.start.date == booking.period.end.date) it else "$it · ${booking.period.start.hhmm()} → ${booking.period.end.hhmm()}"
        }.uppercase(), typography.dataSmall, color = KerbTheme.colors.inkMuted)
    }
}

/** "Marina R. · R$ 70,00 · acerto direto" / "· responde até 12 h" (board 12). */
private fun cardFooter(booking: Booking): String {
    val tail = when (booking.status) {
        BookingStatus.PENDING -> booking.respondBy?.let { "responde até ${it.hhmm()}" } ?: "aguardando resposta"
        BookingStatus.REJECTED, BookingStatus.CANCELLED, BookingStatus.EXPIRED -> null
        else -> "acerto direto"
    }
    return listOfNotNull(booking.counterpartShortName, formatMoney(booking.totalCents), tail).joinToString(" · ")
}

@Composable
private fun EmptyPanel(tab: BookingTab, onAction: (BookingsAction) -> Unit) {
    val (title, text) = when (tab) {
        BookingTab.UPCOMING -> "Nenhuma reserva" to "Escolha um período no Explorar e peça uma vaga a um vizinho."
        BookingTab.ONGOING -> "Nada em curso" to "Quando você fizer o check-in, a reserva aparece aqui."
        BookingTab.HISTORY -> "Histórico vazio" to "Reservas concluídas, canceladas ou recusadas ficam aqui."
    }
    KbPanel(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KbText(text, KerbTheme.typography.body, color = KerbTheme.colors.inkMuted)
            if (tab == BookingTab.UPCOMING) {
                KbButton("Explorar vagas", { onAction(BookingsAction.OnExploreClick) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
