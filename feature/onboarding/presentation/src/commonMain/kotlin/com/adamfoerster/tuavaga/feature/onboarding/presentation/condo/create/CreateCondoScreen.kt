package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbErrorText
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcon
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreenTitle
import com.adamfoerster.tuavaga.core.designsystem.components.KbSectionHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbStepHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.leftBar
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateCondoRoot(
    onResidentData: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateCondoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CreateCondoEvent.GoToResidentData -> onResidentData()
        }
    }

    CreateCondoScreen(
        state = state,
        onAction = { action ->
            if (action == CreateCondoAction.OnBackClick) onBack()
            viewModel.onAction(action)
        },
    )
}

/** Board 20 · Cadastrar meu condomínio. */
@Composable
fun CreateCondoScreen(
    state: CreateCondoState,
    onAction: (CreateCondoAction) -> Unit,
) {
    KbScreen(
        header = {
            KbStepHeader(
                label = "Novo condomínio",
                readout = "Passo 1 de 2",
                step = 1,
                steps = 2,
                onBack = { onAction(CreateCondoAction.OnBackClick) },
            )
        },
        bottomBar = {
            KbText(
                text = "O app não tem painel de síndico. A confiança vem do convite, do perfil e das avaliações.",
                style = KerbTheme.typography.bodySmall,
                color = KerbTheme.colors.inkMuted,
            )
            KbButton(
                text = "Continuar",
                onClick = { onAction(CreateCondoAction.OnSubmit) },
                size = KbButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KbScreenTitle(
            title = "Cadastrar meu condomínio",
            subtitle = "Você será o primeiro morador. Depois, convide os vizinhos com o código.",
        )
        KbField(
            value = state.name,
            onValueChange = { onAction(CreateCondoAction.OnNameChange(it)) },
            label = "Nome do condomínio",
            placeholder = "Residencial Alameda Verde",
            error = state.nameError?.asString(),
        )
        KbField(
            value = state.address,
            onValueChange = { onAction(CreateCondoAction.OnAddressChange(it)) },
            label = "Endereço",
            placeholder = "Rua das Figueiras, 410",
            error = state.addressError?.asString(),
        )
        KbField(
            value = state.cep,
            onValueChange = { onAction(CreateCondoAction.OnCepChange(it)) },
            label = "CEP",
            placeholder = "00000-000",
            error = state.cepError?.asString(),
            keyboardType = KeyboardType.Number,
        )
        KbField(
            value = state.blocks,
            onValueChange = { onAction(CreateCondoAction.OnBlocksChange(it)) },
            label = "Blocos / torres",
            placeholder = "A, B, C",
            hint = "Separe por vírgula. Deixe vazio se não houver blocos.",
        )

        KbSectionHeader("Garagem")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            KbField(
                value = state.levelInput,
                onValueChange = { onAction(CreateCondoAction.OnLevelInputChange(it)) },
                label = "Subsolos e andares",
                placeholder = "Digite e toque em +",
                imeAction = ImeAction.Done,
                onImeAction = { onAction(CreateCondoAction.OnAddLevel) },
                modifier = Modifier.weight(1f),
            )
            KbButton(text = "+", onClick = { onAction(CreateCondoAction.OnAddLevel) }, modifier = Modifier.width(56.dp))
        }
        KbText(
            text = "Use ↑ para ordenar e “+ Setor” para criar setores dentro de cada andar.",
            style = KerbTheme.typography.bodySmall,
            color = KerbTheme.colors.inkMuted,
        )
        KbErrorText(state.levelsError?.asString())
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.levels.forEachIndexed { levelIndex, level ->
                LevelRow(
                    name = level.name,
                    canMoveUp = levelIndex > 0,
                    onMoveUp = { onAction(CreateCondoAction.OnMoveLevelUp(levelIndex)) },
                    onAddSector = { onAction(CreateCondoAction.OnAddSector(levelIndex)) },
                    onRemove = { onAction(CreateCondoAction.OnRemoveLevel(levelIndex)) },
                )
                level.sectors.forEachIndexed { sectorIndex, sector ->
                    SectorRow(
                        name = sector,
                        onRemove = { onAction(CreateCondoAction.OnRemoveSector(levelIndex, sectorIndex)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelRow(
    name: String,
    canMoveUp: Boolean,
    onMoveUp: () -> Unit,
    onAddSector: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = KerbTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(colors.surfaceRaised)
            .leftBar(colors.lineStrong, 3.dp)
            .padding(start = 15.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KbText(name, KerbTheme.typography.body, modifier = Modifier.weight(1f))
        if (canMoveUp) RowIconButton(KbIcons.ArrowUp, "Subir $name", onMoveUp)
        Row(
            modifier = Modifier
                .height(32.dp)
                .border(1.dp, colors.lineStrong)
                .clickable(role = Role.Button, onClick = onAddSector)
                .semantics { contentDescription = "Adicionar setor em $name" }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KbIcon(KbIcons.Plus, contentDescription = null, size = 16.dp)
            KbText("Setor", KerbTheme.typography.dataSmall)
        }
        RowIconButton(KbIcons.Close, "Remover $name", onRemove)
    }
}

@Composable
private fun SectorRow(name: String, onRemove: () -> Unit) {
    val colors = KerbTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp)
            .heightIn(min = 40.dp)
            .background(colors.surfaceSunken)
            .leftBar(colors.apex, 3.dp)
            .padding(start = 15.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KbText("SETOR", KerbTheme.typography.dataSmall, color = colors.inkMuted)
        KbText(name, KerbTheme.typography.body, modifier = Modifier.weight(1f))
        RowIconButton(KbIcons.Close, "Remover setor $name", onRemove)
    }
}

@Composable
private fun RowIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        KbIcon(icon, contentDescription = null, tint = KerbTheme.colors.inkMuted, size = 20.dp)
    }
}
