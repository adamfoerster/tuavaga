package com.adamfoerster.tuavaga.feature.onboarding.presentation.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbBrandHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbHairline
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.presentation.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroRoot(
    onFinished: () -> Unit,
    viewModel: IntroViewModel = koinViewModel(),
) {
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            IntroEvent.Finished -> onFinished()
        }
    }
    IntroScreen(onAction = viewModel::onAction)
}

private class IntroStep(val number: String, val title: String, val text: String)

private val steps = listOf(
    IntroStep("01", "Libere sua vaga", "Escolha dias, horários e preço. Pause quando quiser."),
    IntroStep("02", "Receba o pedido", "Aprove cada reserva ou deixe automático."),
    IntroStep("03", "Faça check-in no app", "Ao chegar, toque em check-in. Sem cobrança no app nesta versão."),
)

/** Board 01 · Introdução. */
@Composable
fun IntroScreen(onAction: (IntroAction) -> Unit) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    KbScreen(
        showZebra = true,
        header = {
            KbBrandHeader {
                KbButton(
                    text = "Pular",
                    onClick = { onAction(IntroAction.OnSkipClick) },
                    variant = KbButtonVariant.Ghost,
                )
            }
        },
        bottomBar = {
            KbButton(
                text = "Continuar",
                onClick = { onAction(IntroAction.OnContinueClick) },
                size = KbButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KbText("Como funciona", typography.lg)
            KbText(
                text = "Vizinhos do mesmo condomínio alugam vagas ociosas por hora, dia ou semana.",
                style = typography.bodyLarge,
                color = colors.inkMuted,
            )
        }
        KbPanel(title = "Três passos") {
            steps.forEachIndexed { index, step ->
                if (index > 0) KbHairline()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        top = if (index == 0) 0.dp else 16.dp,
                        bottom = if (index == steps.lastIndex) 0.dp else 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    KbText(step.number, typography.data, color = colors.apexText, modifier = Modifier.width(28.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        KbText(step.title, typography.sm)
                        KbText(step.text, typography.bodySmall, color = colors.inkMuted)
                    }
                }
            }
        }
    }
}
