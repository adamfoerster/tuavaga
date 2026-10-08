package com.adamfoerster.tuavaga.app.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbText

// Tabs whose features arrive in later phases (see docs/plano-design.md).

@Composable
internal fun ComingSoonTab(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KbPanel(title = title, aside = "EM BREVE") {
            KbText(text, KerbTheme.typography.body, color = KerbTheme.colors.inkMuted)
        }
    }
}
