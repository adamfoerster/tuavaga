package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.join

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbLabeledDivider
import com.adamfoerster.tuavaga.core.designsystem.components.KbListItem
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreenTitle
import com.adamfoerster.tuavaga.core.designsystem.components.KbStepHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun JoinCondoRoot(
    onResidentData: () -> Unit,
    onCreateCondo: () -> Unit,
    onFinished: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: JoinCondoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            JoinCondoEvent.GoToResidentData -> onResidentData()
            JoinCondoEvent.Finished -> onFinished()
        }
    }

    JoinCondoScreen(
        state = state,
        showBack = onBack != null,
        onAction = { action ->
            when (action) {
                JoinCondoAction.OnCreateCondoClick -> onCreateCondo()
                JoinCondoAction.OnBackClick -> onBack?.invoke()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

/** Board 02 · Entrar em um condomínio. */
@Composable
fun JoinCondoScreen(
    state: JoinCondoState,
    showBack: Boolean,
    onAction: (JoinCondoAction) -> Unit,
) {
    KbScreen(
        header = {
            KbStepHeader(
                label = "Passo 1 de 2",
                readout = "Condomínio",
                step = 1,
                steps = 2,
                onBack = if (showBack) ({ onAction(JoinCondoAction.OnBackClick) }) else null,
            )
        },
        bottomBar = {
            KbErrorText(state.error?.asString())
            KbButton(
                text = "Cadastrar meu condomínio",
                onClick = { onAction(JoinCondoAction.OnCreateCondoClick) },
                variant = KbButtonVariant.Ghost,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            KbButton(
                text = "Continuar",
                onClick = { onAction(JoinCondoAction.OnContinueClick) },
                size = KbButtonSize.Large,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbScreenTitle(
            title = "Entrar no condomínio",
            subtitle = "Você só vê e reserva vagas do condomínio ativo. Dá para entrar em mais de um.",
        )
        KbField(
            value = state.query,
            onValueChange = { onAction(JoinCondoAction.OnQueryChange(it)) },
            label = "Nome ou endereço",
            placeholder = "Ex.: Alameda Verde",
            hint = searchHint(state),
            error = state.searchError?.asString(),
            imeAction = ImeAction.Search,
        )
        state.results.forEach { condo ->
            CondoResult(
                condo = condo,
                selected = condo.id == state.selectedId,
                onClick = { onAction(JoinCondoAction.OnSelect(condo.id)) },
            )
        }
        KbLabeledDivider("ou")
        KbField(
            value = state.inviteCode,
            onValueChange = { onAction(JoinCondoAction.OnInviteCodeChange(it)) },
            label = "Código de convite",
            placeholder = "AV-4K7Q",
            hint = "Peça o código ao vizinho que já usa o app.",
            error = state.inviteError?.asString(),
            imeAction = ImeAction.Done,
            onImeAction = { onAction(JoinCondoAction.OnContinueClick) },
        )
    }
}

private fun searchHint(state: JoinCondoState): String? = when {
    state.isSearching -> "Buscando..."
    !state.hasSearched -> null
    state.results.isEmpty() -> "Nenhum condomínio encontrado. Confira o nome ou cadastre o seu."
    state.results.size == 1 -> "1 condomínio encontrado"
    else -> "${state.results.size} condomínios encontrados"
}

@Composable
private fun CondoResult(condo: CondoPreview, selected: Boolean, onClick: () -> Unit) {
    val towers = when (condo.blocksCount) {
        0 -> null
        1 -> "1 torre"
        else -> "${condo.blocksCount} torres"
    }
    KbListItem(
        title = condo.name,
        meta = listOfNotNull(condo.address, towers).joinToString(" · "),
        text = when (condo.listedSpots) {
            0 -> "Nenhuma vaga anunciada ainda"
            1 -> "1 vaga anunciada por vizinhos"
            else -> "${condo.listedSpots} vagas anunciadas por vizinhos"
        },
        selected = selected,
        onClick = onClick,
        trailing = when {
            selected -> ({ KbTag("Selecionado", tone = KbTone.Go) })
            condo.isMember -> ({ KbTag("Você já participa", tone = KbTone.Info) })
            else -> null
        },
    )
    if (selected && condo.isMember) {
        KbText(
            text = "Você já participa deste condomínio. Continuar torna ele o ativo.",
            style = KerbTheme.typography.bodySmall,
            color = KerbTheme.colors.inkMuted,
        )
    }
}
