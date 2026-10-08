package com.adamfoerster.tuavaga.feature.messages.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbShapes
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbAvatar
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbChip
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.bottomBar
import com.adamfoerster.tuavaga.core.designsystem.components.initialsOf
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.presentation.counterpartShortName
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.statusTag
import com.adamfoerster.tuavaga.core.presentation.weekdayDayMonth
import com.adamfoerster.tuavaga.feature.messages.domain.QUICK_REPLIES
import com.adamfoerster.tuavaga.feature.messages.presentation.common.periodShort
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Composable
fun ChatRoot(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenBooking: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ChatAction.OnBackClick -> onBack()
                ChatAction.OnBookingClick -> onOpenBooking()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** One row of the conversation: a day separator, a stored message or one being sent. */
private sealed interface ChatRow {
    val key: String

    data class Day(val date: LocalDate) : ChatRow {
        override val key = "day-$date"
    }

    data class Stored(val id: String, val body: String, val mine: Boolean, val system: Boolean, val at: LocalDateTime) : ChatRow {
        override val key = "m-$id"
    }

    data class Sending(val pending: PendingMessage) : ChatRow {
        override val key = "p-${pending.localId}"
    }
}

private fun ChatState.rows(): List<ChatRow> {
    val all = messages.map { ChatRow.Stored(it.id, it.body, it.isMine, it.isSystem, it.at) } +
        pending.map { ChatRow.Sending(it) }
    val rows = mutableListOf<ChatRow>()
    var day: LocalDate? = null
    all.forEach { row ->
        val date = when (row) {
            is ChatRow.Stored -> row.at.date
            is ChatRow.Sending -> row.pending.at.date
            is ChatRow.Day -> row.date
        }
        if (date != day) {
            rows += ChatRow.Day(date)
            day = date
        }
        rows += row
    }
    return rows
}

/** Board 23 · Chat da reserva. */
@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val colors = KerbTheme.colors
    val rows = state.rows()
    val listState = rememberLazyListState()
    LaunchedEffect(rows.size) { if (rows.isNotEmpty()) listState.animateScrollToItem(rows.lastIndex) }
    Box(modifier = Modifier.fillMaxSize().background(colors.surface), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp).safeDrawingPadding().imePadding()) {
            ChatHeader(state.booking, onAction)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading && rows.isEmpty() ->
                        KbMeter(value = 0.4f, label = "Carregando conversa", segments = 24, redline = 1f, modifier = Modifier.padding(16.dp))
                    rows.isEmpty() -> KbText(
                        "Comece a conversa. ${state.booking?.counterpartShortName ?: "O vizinho"} recebe na hora.",
                        KerbTheme.typography.body,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        items(rows, key = { it.key }) { row -> ChatRowView(row, onAction) }
                    }
                }
            }
            Composer(state, onAction)
        }
    }
}

@Composable
private fun ChatHeader(booking: Booking?, onAction: (ChatAction) -> Unit) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth().bottomBar(colors.line, 2.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KbIconButton(KbIcons.Back, "Voltar", { onAction(ChatAction.OnBackClick) })
        if (booking == null) return@Row
        KbAvatar(initialsOf(booking.counterpart.name ?: booking.counterpartShortName), size = 40.dp)
        Column(
            modifier = Modifier.weight(1f).clickable { onAction(ChatAction.OnBookingClick) },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            KbText(booking.counterpartShortName, typography.sm, maxLines = 1, overflow = TextOverflow.Ellipsis)
            KbText(
                "Reserva ${booking.code} · ${booking.spotCode} · ${booking.period.periodShort()}".uppercase(),
                typography.dataSmall,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Below the name, so a long name never pushes it out (board 23 header).
            val (status, tone) = booking.statusTag()
            KbTag(status, tone = tone)
        }
    }
}

@Composable
private fun ChatRowView(row: ChatRow, onAction: (ChatAction) -> Unit) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    when (row) {
        is ChatRow.Day -> KbText(
            row.date.weekdayDayMonth().uppercase(),
            typography.dataSmall,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        is ChatRow.Stored -> if (row.system) {
            KbText(
                row.body,
                typography.dataSmall,
                color = colors.telemetry,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        } else {
            Bubble(row.body, row.at.hhmm(), mine = row.mine)
        }
        is ChatRow.Sending -> Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Bubble(row.pending.body, if (row.pending.failed) "não enviada" else "enviando…", mine = true, faded = !row.pending.failed)
            if (row.pending.failed) {
                KbText(
                    "Tentar de novo",
                    typography.label,
                    color = colors.danger,
                    modifier = Modifier.clickable { onAction(ChatAction.OnRetry(row.pending.localId)) }.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Bubble(body: String, meta: String, mine: Boolean, faded: Boolean = false) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(if (mine) colors.apex.copy(alpha = if (faded) 0.6f else 1f) else colors.surfaceRaised, KerbShapes.chamferSm)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            KbText(body, typography.body, color = if (mine) colors.onApex else colors.ink)
            KbText(meta, typography.dataSmall, color = if (mine) colors.onApex.copy(alpha = 0.7f) else colors.inkMuted)
        }
    }
}

@Composable
private fun Composer(state: ChatState, onAction: (ChatAction) -> Unit) {
    val colors = KerbTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.surface).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KbErrorText(state.error?.asString())
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_REPLIES.forEach { reply ->
                KbChip(text = reply, selected = false, onClick = { onAction(ChatAction.OnQuickReply(reply)) })
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KbField(
                value = state.draft,
                onValueChange = { onAction(ChatAction.OnDraftChange(it)) },
                label = "Mensagem",
                placeholder = state.booking?.let { "Escreva para ${it.counterpartShortName}" } ?: "Escreva uma mensagem",
                singleLine = false,
                imeAction = ImeAction.Default,
                modifier = Modifier.weight(1f),
            )
            KbButton("Enviar", { onAction(ChatAction.OnSendClick) }, enabled = state.canSend)
        }
    }
}
