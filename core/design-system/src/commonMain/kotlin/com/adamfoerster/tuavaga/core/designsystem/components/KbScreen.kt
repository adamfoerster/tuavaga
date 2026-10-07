package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

/** Name shown in the app header. The design says "Vaga Vizinha"; the product is TuaVaga. */
const val APP_DISPLAY_NAME = "TuaVaga"

/**
 * Base layout of a phone screen in the design: Kerb surface, optional hazard stripe, [header],
 * scrolling [content] (16 dp side gutters, 16 dp gaps) and a fixed [bottomBar] for the main actions.
 * On wide windows (web) the column is centered and capped at 480 dp.
 */
@Composable
fun KbScreen(
    modifier: Modifier = Modifier,
    showZebra: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KerbTheme.colors.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .safeDrawingPadding()
                .imePadding(),
        ) {
            if (showZebra) KbZebraStripe()
            header?.invoke()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
            if (bottomBar != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = bottomBar,
                )
            }
        }
    }
}

/** Brand row of the access screens: app name on the left, optional [trailing] action. */
@Composable
fun KbBrandHeader(trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KbText(APP_DISPLAY_NAME, KerbTheme.typography.sm, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** Big screen title (`t-lg`) with an optional muted subtitle. */
@Composable
fun KbScreenTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KbText(title, KerbTheme.typography.lg)
        if (subtitle != null) {
            KbText(subtitle, KerbTheme.typography.body, color = KerbTheme.colors.inkMuted)
        }
    }
}
