package com.adamfoerster.tuavaga.app.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbPanel
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbZebraStripe
import com.adamfoerster.tuavaga.core.domain.condo.Membership

// Tabs whose features arrive in later phases (see docs/plano-design.md).

@Composable
private fun TabColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** Edge state "Condomínio sem vagas" (board 17) until spots exist. */
@Composable
internal fun ExplorePlaceholder(active: Membership?, onListSpot: () -> Unit) {
    val typography = KerbTheme.typography
    val colors = KerbTheme.colors
    TabColumn {
        KbPanel(title = "Condomínio sem vagas", aside = "EXPLORAR · NOVO") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KbZebraStripe(Modifier.height(12.dp))
                KbText("Seja o primeiro", typography.md)
                KbText(
                    text = "Ninguém anunciou vaga no ${active?.condo?.name.orEmpty()} ainda. " +
                        "Anuncie a sua e convide os vizinhos.",
                    style = typography.body,
                    color = colors.inkMuted,
                )
                Text(
                    text = buildAnnotatedString {
                        append("CÓDIGO DE CONVITE · ")
                        withStyle(SpanStyle(color = colors.telemetry)) { append(active?.condo?.inviteCode.orEmpty()) }
                    },
                    style = typography.data,
                    color = colors.ink,
                )
                KbButton(text = "Anunciar minha vaga", onClick = onListSpot, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun ComingSoonTab(title: String, text: String) {
    TabColumn {
        KbPanel(title = title, aside = "EM BREVE") {
            KbText(text, KerbTheme.typography.body, color = KerbTheme.colors.inkMuted)
        }
    }
}
