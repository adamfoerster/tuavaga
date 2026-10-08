package com.adamfoerster.tuavaga.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbField
import com.adamfoerster.tuavaga.core.designsystem.components.KbOption
import com.adamfoerster.tuavaga.core.designsystem.components.KbSelect
import com.adamfoerster.tuavaga.core.domain.booking.RejectReason

/** The owner refusing a request (board 17 "Recusando"): reason is required, message optional. */
data class RejectDraft(
    val bookingId: String,
    val reason: RejectReason? = null,
    val message: String = "",
) {
    val canConfirm: Boolean get() = reason != null && message.length <= MESSAGE_MAX

    companion object {
        const val MESSAGE_MAX = 280
    }
}

@Composable
fun RejectForm(
    draft: RejectDraft,
    isWorking: Boolean,
    onReason: (RejectReason) -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KbSelect(
            label = "Motivo da recusa",
            options = RejectReason.entries.map { KbOption(it, it.label) },
            selected = draft.reason,
            onSelect = onReason,
        )
        KbField(
            value = draft.message,
            onValueChange = { onMessage(it.take(RejectDraft.MESSAGE_MAX)) },
            label = "Mensagem (opcional)",
            placeholder = "Posso liberar outro dia.",
            singleLine = false,
            imeAction = ImeAction.Default,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KbButton("Voltar", onBack, modifier = Modifier.weight(1f), variant = KbButtonVariant.Ghost, enabled = !isWorking)
            KbButton(
                text = "Confirmar recusa",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = draft.canConfirm,
                isLoading = isWorking,
            )
        }
    }
}
