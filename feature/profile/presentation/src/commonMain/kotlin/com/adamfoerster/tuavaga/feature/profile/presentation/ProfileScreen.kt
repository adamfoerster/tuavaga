package com.adamfoerster.tuavaga.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.adamfoerster.tuavaga.core.designsystem.components.KbAvatar
import com.adamfoerster.tuavaga.core.designsystem.components.KbBottomSheet
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbHairline
import com.adamfoerster.tuavaga.core.designsystem.components.KbListItem
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbReadout
import com.adamfoerster.tuavaga.core.designsystem.components.KbSelect
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.KbToolbar
import com.adamfoerster.tuavaga.core.designsystem.components.initialsOf
import com.adamfoerster.tuavaga.core.designsystem.components.leftBar
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.vehicle.VEHICLE_COLORS
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import org.koin.compose.viewmodel.koinViewModel

/** Content of the Perfil tab (inside the app shell). */
@Composable
fun ProfileRoot(
    onAddCondo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        modifier = modifier,
        onAction = { action ->
            if (action == ProfileAction.OnAddCondoClick) onAddCondo()
            viewModel.onAction(action)
        },
    )
}

private val typeOptions = listOf(
    KbOption(VehicleType.CAR, "Carro"),
    KbOption(VehicleType.MOTORCYCLE, "Moto"),
    KbOption(VehicleType.LARGE, "Grande porte"),
)

private fun VehicleType.label(): String = typeOptions.first { it.value == this }.label

/** "MORADOR · BL. B 142" / "TRABALHO · SALA 3-15" (board 24). */
internal fun Membership.roleLine(): String = when (kind) {
    MembershipKind.RESIDENT -> listOfNotNull("Morador", listOfNotNull(block?.let { "Bl. $it" }, unit).joinToString(" ").ifEmpty { null })
    MembershipKind.WORK -> listOf("Trabalho", "Sala $unit")
}.joinToString(" · ").uppercase()

/** Board 24 · Perfil. */
@Composable
fun ProfileScreen(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    val name = state.userName?.takeIf { it.isNotBlank() } ?: state.userEmail
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // Room for the error strip, so it never hides the last buttons.
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (state.error != null) 112.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                KbAvatar(initialsOf(name), size = 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    KbText(name, typography.md)
                    KbText(
                        state.active?.let { a -> listOfNotNull(a.block?.let { "Bloco $it" }, "Unidade ${a.unit}").joinToString(" · ") }
                            ?.uppercase() ?: state.userEmail,
                        typography.dataSmall,
                        color = colors.inkMuted,
                    )
                }
            }
            KbReadout(label = "Reservas", value = "${state.bookingCount}", unit = "feitas")
            VehiclesPanel(state, onAction)
            CondosPanel(state, onAction)
            KbPanel(title = "Mais") {
                Column {
                    listOf("Preferências de notificação", "Ajuda e regras gerais", "Denunciar problema ao suporte").forEachIndexed { i, item ->
                        if (i > 0) KbHairline()
                        KbListItem(title = item, trailing = { KbTag("Em breve") })
                    }
                }
            }
            KbButton(
                text = "Sair da conta",
                onClick = { onAction(ProfileAction.OnSignOutClick) },
                variant = KbButtonVariant.Ghost,
                isLoading = state.isSigningOut,
                modifier = Modifier.fillMaxWidth(),
            )
            DeleteAccount(state, onAction)
        }
        val error = state.error
        // Fixed at the bottom: the actions that fail (leave, delete) are far from the top of the list.
        if (error != null && state.vehicleDraft == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(colors.surfaceRaised)
                    .leftBar(colors.danger, 4.dp)
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KbText(error.asString(), typography.body, color = colors.danger, modifier = Modifier.weight(1f))
                KbButton("OK", { onAction(ProfileAction.OnErrorDismiss) }, variant = KbButtonVariant.Ghost, size = KbButtonSize.Small)
            }
        }
        state.vehicleDraft?.let { VehicleSheet(it, state.isWorking, state.error?.asString(), onAction) }
    }
}

@Composable
private fun VehiclesPanel(state: ProfileState, onAction: (ProfileAction) -> Unit) {
    KbPanel(title = "Veículos", aside = "${state.vehicles.size}") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                state.vehicles.isEmpty() && state.isLoadingVehicles ->
                    KbText("Carregando…", KerbTheme.typography.bodySmall, color = KerbTheme.colors.inkMuted)
                state.vehicles.isEmpty() ->
                    KbText("Nenhum veículo. Cadastre para pedir vagas.", KerbTheme.typography.bodySmall, color = KerbTheme.colors.inkMuted)
            }
            state.vehicles.forEachIndexed { i, v ->
                if (i > 0) KbHairline()
                VehicleRow(v, onClick = { onAction(ProfileAction.OnVehicleClick(v.id)) })
            }
            KbButton(
                text = "Adicionar veículo",
                onClick = { onAction(ProfileAction.OnAddVehicleClick) },
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun VehicleRow(vehicle: Vehicle, onClick: () -> Unit) {
    KbListItem(
        title = vehicle.plate,
        text = "${vehicle.model} · ${vehicle.color.lowercase()}",
        onClick = onClick,
        trailing = { KbTag(vehicle.type.label()) },
    )
}

@Composable
private fun CondosPanel(state: ProfileState, onAction: (ProfileAction) -> Unit) {
    val activeId = state.active?.condo?.id
    KbPanel(title = "Condomínios", aside = "${state.memberships.size}") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.memberships.forEachIndexed { i, m ->
                if (i > 0) KbHairline()
                val isActive = m.condo.id == activeId
                KbListItem(
                    title = m.condo.name,
                    meta = m.roleLine(),
                    selected = isActive,
                    onClick = { onAction(ProfileAction.OnCondoSelect(m.condo.id)) },
                    trailing = {
                        if (isActive) {
                            KbTag("Ativo", tone = KbTone.Go)
                        } else {
                            KbButton("Sair", { onAction(ProfileAction.OnLeaveClick(m.condo.id)) }, variant = KbButtonVariant.Ghost, size = KbButtonSize.Small)
                        }
                    },
                )
                if (state.leavingCondoId == m.condo.id) {
                    LeaveConfirm(m, state.isWorking, onAction)
                }
            }
            if (state.memberships.size > 1) {
                KbText(
                    "Para sair do condomínio ativo, ative outro antes.",
                    KerbTheme.typography.bodySmall,
                    color = KerbTheme.colors.inkMuted,
                )
            }
            KbButton(
                text = "Adicionar condomínio",
                onClick = { onAction(ProfileAction.OnAddCondoClick) },
                variant = KbButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LeaveConfirm(membership: Membership, isWorking: Boolean, onAction: (ProfileAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KbText(
            "Sair de ${membership.condo.name}? Suas vagas lá ficam pausadas e você precisa do convite para voltar.",
            KerbTheme.typography.body,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbButton("Voltar", { onAction(ProfileAction.OnLeaveDismiss) }, Modifier.weight(1f), variant = KbButtonVariant.Ghost)
            KbButton("Sair", { onAction(ProfileAction.OnLeaveConfirm) }, Modifier.weight(1f), isLoading = isWorking)
        }
    }
}

@Composable
private fun DeleteAccount(state: ProfileState, onAction: (ProfileAction) -> Unit) {
    if (!state.isConfirmingDelete) {
        KbButton(
            text = "Excluir conta",
            onClick = { onAction(ProfileAction.OnDeleteAccountClick) },
            variant = KbButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    KbPanel(title = "Excluir conta?") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KbTag("Não dá para desfazer", tone = KbTone.Danger)
            KbText(
                "Suas reservas futuras são canceladas (os vizinhos são avisados) e seus dados, vagas, veículos e " +
                    "conversas são apagados.",
                KerbTheme.typography.body,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KbButton("Voltar", { onAction(ProfileAction.OnDeleteDismiss) }, Modifier.weight(1f), variant = KbButtonVariant.Ghost)
                KbButton("Excluir", { onAction(ProfileAction.OnDeleteConfirm) }, Modifier.weight(1f), isLoading = state.isWorking)
            }
        }
    }
}

@Composable
private fun VehicleSheet(draft: VehicleDraft, isWorking: Boolean, error: String?, onAction: (ProfileAction) -> Unit) {
    KbBottomSheet(onDismiss = { onAction(ProfileAction.OnCloseVehicleSheet) }) {
        KbText(if (draft.id == null) "Novo veículo" else "Editar veículo", KerbTheme.typography.lg)
        KbField(
            value = draft.plate,
            onValueChange = { onAction(ProfileAction.OnPlateChange(it)) },
            label = "Placa",
            placeholder = "ABC1D23",
            error = draft.plateError?.asString(),
        )
        KbField(
            value = draft.model,
            onValueChange = { onAction(ProfileAction.OnModelChange(it)) },
            label = "Modelo",
            placeholder = "Chevrolet Onix",
            error = draft.modelError?.asString(),
        )
        KbSelect(
            label = "Cor",
            options = VEHICLE_COLORS.let { if (draft.color != null && draft.color !in it) it + draft.color else it }.map { KbOption(it, it) },
            selected = draft.color,
            onSelect = { onAction(ProfileAction.OnColorSelect(it)) },
            error = draft.colorError?.asString(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KbText("Tipo", KerbTheme.typography.label, color = KerbTheme.colors.inkMuted)
            KbToolbar(options = typeOptions, selected = draft.type, onSelect = { onAction(ProfileAction.OnTypeSelect(it)) })
        }
        KbErrorText(error)
        KbButton(
            text = "Salvar",
            onClick = { onAction(ProfileAction.OnSaveVehicle) },
            isLoading = isWorking && !draft.isConfirmingRemove,
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.id != null) {
            if (draft.isConfirmingRemove) {
                KbText("Remover ${draft.plate}?", KerbTheme.typography.body)
                KbButton(
                    text = "Remover",
                    onClick = { onAction(ProfileAction.OnRemoveVehicleConfirm) },
                    variant = KbButtonVariant.Ghost,
                    isLoading = isWorking,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                KbButton(
                    text = "Remover veículo",
                    onClick = { onAction(ProfileAction.OnRemoveVehicleClick) },
                    variant = KbButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
