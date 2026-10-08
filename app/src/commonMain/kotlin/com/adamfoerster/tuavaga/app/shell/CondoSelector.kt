package com.adamfoerster.tuavaga.app.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme
import com.adamfoerster.tuavaga.core.designsystem.components.KbButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonSize
import com.adamfoerster.tuavaga.core.designsystem.components.KbButtonVariant
import com.adamfoerster.tuavaga.core.designsystem.components.KbIconButton
import com.adamfoerster.tuavaga.core.designsystem.components.KbIcons
import com.adamfoerster.tuavaga.core.designsystem.components.KbListItem
import com.adamfoerster.tuavaga.core.designsystem.components.KbTag
import com.adamfoerster.tuavaga.core.designsystem.components.KbText
import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.bottomBar
import com.adamfoerster.tuavaga.core.designsystem.components.leftBar
import com.adamfoerster.tuavaga.core.designsystem.components.topBar
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind

/** "BLOCO B · UNIDADE 142 · MORADOR" */
internal fun Membership.placeLine(): String = listOfNotNull(
    block?.let { "Bloco $it" },
    if (kind == MembershipKind.WORK) "Sala $unit" else "Unidade $unit",
    if (kind == MembershipKind.WORK) "Trabalho" else "Morador",
).joinToString(" · ")

/** Board "Extensão · Seletor de condomínio": the active condominium, tap to switch. */
@Composable
/** [unread] notifications show on the bell next to it (board "Seletor de condomínio" badge). */
internal fun CondoSelectorBar(active: Membership?, unread: Int, onClick: () -> Unit, onNotifications: () -> Unit) {
    val colors = KerbTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(colors.surface)
            .bottomBar(colors.line, 2.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .background(colors.surfaceRaised)
                .leftBar(colors.apex, 4.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Trocar de condomínio" }
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                KbText("Condomínio ativo", KerbTheme.typography.label, color = colors.inkMuted)
                KbText(
                    text = active?.condo?.name.orEmpty(),
                    style = KerbTheme.typography.sm,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            KbText("▼", KerbTheme.typography.dataSmall, color = colors.inkMuted)
        }
        KbIconButton(
            icon = KbIcons.Bell,
            contentDescription = if (unread > 0) "Notificações, $unread novas" else "Notificações",
            onClick = onNotifications,
            badge = unread,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Board 04 · Troca de condomínio: bottom sheet over the current screen. */
@Composable
internal fun CondoSwitcherSheet(
    memberships: List<Membership>,
    activeId: String?,
    unreadByCondo: Map<String, Int>,
    onSelect: (String) -> Unit,
    onAddCondo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KerbTheme.colors
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xA6000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Fechar",
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .background(colors.surfaceRaised)
                .topBar(colors.apex, 3.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .background(colors.lineStrong),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                KbText("Seus condomínios", KerbTheme.typography.md, modifier = Modifier.weight(1f))
                KbText(
                    text = if (memberships.size == 1) "1 VÍNCULO" else "${memberships.size} VÍNCULOS",
                    style = KerbTheme.typography.dataSmall,
                    color = colors.inkMuted,
                )
            }
            memberships.forEach { membership ->
                val isActive = membership.condo.id == activeId
                val unread = unreadByCondo[membership.condo.id] ?: 0
                KbListItem(
                    title = membership.condo.name,
                    meta = membership.placeLine(),
                    text = "Código de convite · ${membership.condo.inviteCode}",
                    selected = isActive,
                    glow = true,
                    onClick = { onSelect(membership.condo.id) },
                    trailing = when {
                        unread > 0 -> ({ KbTag(if (unread == 1) "1 nova" else "$unread novas", tone = KbTone.Danger) })
                        isActive -> ({ KbTag("Ativo", tone = KbTone.Go) })
                        else -> null
                    },
                )
            }
            KbButton(
                text = "Adicionar outro condomínio",
                onClick = onAddCondo,
                variant = KbButtonVariant.Ghost,
                size = KbButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
