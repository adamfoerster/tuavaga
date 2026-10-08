package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.resident

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbShapes
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbCheckRow
import com.adamfoerster.tuavaga.core.designsystem.components.KbChip
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbListItem
import com.adamfoerster.tuavaga.core.designsystem.components.KbMeter
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbPhotoPlaceholder
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbSectionHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbSelect
import com.adamfoerster.tuavaga.core.designsystem.components.KbStepHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.KbToolbar
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResidentDataRoot(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: ResidentDataViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ResidentDataEvent.Finished -> onFinished()
        }
    }

    ResidentDataScreen(
        state = state,
        onAction = { action ->
            if (action == ResidentDataAction.OnBackClick) onBack()
            viewModel.onAction(action)
        },
    )
}

private val kindOptions = listOf(
    KbOption(MembershipKind.RESIDENT, "Morador"),
    KbOption(MembershipKind.WORK, "Trabalho"),
)

private val typeOptions = listOf(
    KbOption(VehicleType.CAR, "Carro"),
    KbOption(VehicleType.MOTORCYCLE, "Moto"),
    KbOption(VehicleType.LARGE, "Grande porte"),
)

internal fun VehicleType.label(): String = typeOptions.first { it.value == this }.label

/** Board 03 · Cadastro do morador e veículo. */
@Composable
fun ResidentDataScreen(
    state: ResidentDataState,
    onAction: (ResidentDataAction) -> Unit,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    KbScreen(
        header = {
            KbStepHeader(
                label = if (state.isNewCondo) "Novo condomínio" else "Passo 2 de 2",
                readout = if (state.isNewCondo) "Passo 2 de 2" else "Seus dados",
                step = 2,
                steps = 2,
                onBack = { onAction(ResidentDataAction.OnBackClick) },
            )
        },
        bottomBar = {
            KbCheckRow(
                checked = state.termsAccepted,
                onCheckedChange = { onAction(ResidentDataAction.OnTermsChange(it)) },
                text = "Li e aceito os termos de uso e as regras gerais do app. " +
                    "Cada locador define as regras da própria vaga.",
                enabled = !state.isSubmitting,
            )
            KbErrorText(state.termsError?.asString())
            KbErrorText(state.error?.asString())
            if (state.error != null && state.condoName.isEmpty() && !state.isLoading) {
                KbButton(
                    text = "Tentar de novo",
                    onClick = { onAction(ResidentDataAction.OnRetryLoad) },
                    variant = KbButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            KbButton(
                text = if (state.isNewCondo) "Criar condomínio" else "Concluir cadastro",
                onClick = { onAction(ResidentDataAction.OnSubmit) },
                size = KbButtonSize.Large,
                isLoading = state.isSubmitting,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        if (state.isLoading) {
            KbMeter(value = 0.4f, label = "Carregando", segments = 24, redline = 1f)
            return@KbScreen
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.condoName.isNotEmpty()) KbTag(state.condoName, tone = KbTone.Info)
            KbText("Seus dados no condomínio", typography.lg)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            KbPhotoPlaceholder("Foto", Modifier.size(80.dp).clip(KerbShapes.chamferMd))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KbText("Foto do perfil", typography.sm)
                KbText(
                    text = "Em breve. Vizinhos verão sua foto, bloco e unidade antes de aprovar um pedido.",
                    style = typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        }
        KbField(
            value = state.fullName,
            onValueChange = { onAction(ResidentDataAction.OnFullNameChange(it)) },
            label = "Nome",
            error = state.nameError?.asString(),
            enabled = !state.isSubmitting,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KbText("Vínculo", typography.label, color = colors.inkMuted)
            KbToolbar(
                options = kindOptions,
                selected = state.kind,
                onSelect = { onAction(ResidentDataAction.OnKindSelect(it)) },
                enabled = !state.isSubmitting,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.blocks.isNotEmpty()) {
                KbSelect(
                    label = "Bloco / torre",
                    options = state.blocks.map { KbOption(it, it) },
                    selected = state.block,
                    onSelect = { onAction(ResidentDataAction.OnBlockSelect(it)) },
                    error = state.blockError?.asString(),
                    enabled = !state.isSubmitting,
                    modifier = Modifier.weight(1f),
                )
            }
            KbField(
                value = state.unit,
                onValueChange = { onAction(ResidentDataAction.OnUnitChange(it)) },
                label = if (state.kind == MembershipKind.WORK) "Sala / unidade" else "Unidade",
                error = state.unitError?.asString(),
                enabled = !state.isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
        KbField(
            value = state.phone,
            onValueChange = { onAction(ResidentDataAction.OnPhoneChange(it)) },
            label = "Telefone",
            placeholder = "(00) 90000-0000",
            hint = "Só aparece para quem tem reserva com você.",
            error = state.phoneError?.asString(),
            keyboardType = KeyboardType.Phone,
            enabled = !state.isSubmitting,
        )

        KbSectionHeader(if (state.existingVehicles.size + state.vehicleDrafts.size > 1) "Veículos" else "Veículo")
        state.existingVehicles.forEach { vehicle ->
            KbListItem(
                title = "${vehicle.plate} · ${vehicle.model}",
                meta = "${vehicle.type.label()} · ${vehicle.color}",
                trailing = { KbTag("Cadastrado", tone = KbTone.Go) },
            )
        }
        if (state.existingVehicles.isEmpty()) {
            KbText(
                text = "Opcional se você só vai anunciar vaga. Para reservar, cadastre o veículo.",
                style = typography.bodySmall,
                color = colors.inkMuted,
            )
        }
        state.vehicleDrafts.forEachIndexed { index, draft ->
            VehicleForm(
                index = index,
                draft = draft,
                removable = state.vehicleDrafts.size > 1 || state.existingVehicles.isNotEmpty(),
                enabled = !state.isSubmitting,
                onAction = onAction,
            )
        }
        KbChip(
            text = "+ Adicionar ${if (state.existingVehicles.isEmpty() && state.vehicleDrafts.isEmpty()) "veículo" else "outro veículo"}",
            selected = false,
            onClick = { onAction(ResidentDataAction.OnAddVehicle) },
        )
    }
}

@Composable
private fun VehicleForm(
    index: Int,
    draft: VehicleDraft,
    removable: Boolean,
    enabled: Boolean,
    onAction: (ResidentDataAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (removable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KbText("Novo veículo", KerbTheme.typography.label, color = KerbTheme.colors.inkMuted, modifier = Modifier.weight(1f))
                KbButton(
                    text = "Remover",
                    onClick = { onAction(ResidentDataAction.OnRemoveVehicle(index)) },
                    variant = KbButtonVariant.Ghost,
                    size = KbButtonSize.Small,
                    enabled = enabled,
                )
            }
        }
        KbField(
            value = draft.plate,
            onValueChange = { onAction(ResidentDataAction.OnPlateChange(index, it)) },
            label = "Placa",
            placeholder = "ABC1D23",
            error = draft.plateError?.asString(),
            enabled = enabled,
        )
        KbField(
            value = draft.model,
            onValueChange = { onAction(ResidentDataAction.OnModelChange(index, it)) },
            label = "Modelo",
            placeholder = "Chevrolet Onix",
            error = draft.modelError?.asString(),
            enabled = enabled,
        )
        KbSelect(
            label = "Cor",
            options = VEHICLE_COLORS.map { KbOption(it, it) },
            selected = draft.color,
            onSelect = { onAction(ResidentDataAction.OnColorSelect(index, it)) },
            error = draft.colorError?.asString(),
            enabled = enabled,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KbText("Tipo", KerbTheme.typography.label, color = KerbTheme.colors.inkMuted)
            KbToolbar(
                options = typeOptions,
                selected = draft.type,
                onSelect = { onAction(ResidentDataAction.OnTypeSelect(index, it)) },
                enabled = enabled,
            )
        }
    }
}
