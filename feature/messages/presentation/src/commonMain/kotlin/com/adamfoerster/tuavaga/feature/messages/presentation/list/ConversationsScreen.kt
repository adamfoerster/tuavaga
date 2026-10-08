package com.adamfoerster.tuavaga.feature.messages.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbAvatar
import com.adamfoerster.tuavaga.core.designsystem.components.KbCard
import com.adamfoerster.tuavaga.core.designsystem.components.KbCountBadge
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.initialsOf
import com.adamfoerster.tuavaga.core.domain.user.shortName
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.dayMonth
import com.adamfoerster.tuavaga.core.presentation.hhmm
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.messages.domain.Conversation
import com.adamfoerster.tuavaga.feature.messages.domain.MessagesRepository
import com.adamfoerster.tuavaga.feature.messages.presentation.common.periodShort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

data class ConversationsState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    /** Last load failed (the previous list stays). */
    val error: UiText? = null,
)

/** Aba Mensagens: one conversation per booking, kept current by Realtime. */
class ConversationsViewModel(messagesRepository: MessagesRepository) : ViewModel() {

    private val _state = MutableStateFlow(ConversationsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            messagesRepository.conversations.collect { result ->
                _state.update {
                    when (result) {
                        is Result.Success -> it.copy(conversations = result.data, isLoading = false, error = null)
                        is Result.Failure -> it.copy(isLoading = false, error = result.error.toUiText())
                    }
                }
            }
        }
    }
}

/** Content of the Mensagens tab (inside the app shell). */
@Composable
fun ConversationsRoot(
    onOpenChat: (bookingId: String) -> Unit,
    today: LocalDate,
    modifier: Modifier = Modifier,
    viewModel: ConversationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConversationsScreen(state, today, onOpenChat, modifier)
}

@Composable
fun ConversationsScreen(
    state: ConversationsState,
    today: LocalDate,
    onOpenChat: (bookingId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KerbTheme.typography
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KbText("Mensagens", typography.lg)
        KbErrorText(state.error?.asString())
        when {
            state.conversations.isEmpty() && state.isLoading ->
                KbMeter(value = 0.4f, label = "Carregando conversas", segments = 24, redline = 1f)
            state.conversations.isEmpty() -> KbPanel(title = "Nenhuma conversa") {
                KbText(
                    "Cada reserva tem um chat com o vizinho. Ele aparece aqui quando você pede ou recebe uma reserva.",
                    typography.body,
                    color = KerbTheme.colors.inkMuted,
                )
            }
            else -> state.conversations.forEach { ConversationCard(it, today, onClick = { onOpenChat(it.bookingId) }) }
        }
    }
}

@Composable
private fun ConversationCard(conversation: Conversation, today: LocalDate, onClick: () -> Unit) {
    val typography = KerbTheme.typography
    val muted = KerbTheme.colors.inkMuted
    val name = shortName(conversation.counterpartName) ?: "Vizinho"
    KbCard(onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KbAvatar(initialsOf(conversation.counterpartName ?: name), size = 44.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KbText(name, typography.md, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    conversation.last?.let {
                        KbText(if (it.at.date == today) it.at.hhmm() else it.at.date.dayMonth(), typography.dataSmall, color = muted)
                    }
                }
                KbText(
                    "Reserva ${conversation.code} · ${conversation.spotLabel} · ${conversation.period.periodShort()}".uppercase(),
                    typography.dataSmall,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KbText(
                        text = conversation.last?.let { last ->
                            when {
                                last.isSystem -> last.body
                                last.isMine -> "Você: ${last.body}"
                                else -> last.body
                            }
                        } ?: "Sem mensagens ainda",
                        style = typography.bodySmall,
                        color = if (conversation.unread > 0) KerbTheme.colors.ink else muted,
                        maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (conversation.unread > 0) KbCountBadge(conversation.unread)
                }
            }
        }
    }
}
