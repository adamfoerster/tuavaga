package com.adamfoerster.tuavaga.feature.auth.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.adamfoerster.tuavaga.core.designsystem.components.KbBrandHeader
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreen
import com.adamfoerster.tuavaga.core.designsystem.components.KbScreenTitle

/** Layout shared by the access screens (board 19): hazard stripe, brand, big title, actions at the bottom. */
@Composable
internal fun AuthScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backEnabled: Boolean = true,
    bottomBar: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    KbScreen(
        showZebra = true,
        header = {
            KbBrandHeader(
                trailing = onBack?.let { back ->
                    {
                        KbButton(
                            text = "Voltar",
                            onClick = back,
                            variant = KbButtonVariant.Ghost,
                            enabled = backEnabled,
                        )
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) {
        KbScreenTitle(title, subtitle)
        content()
    }
}
