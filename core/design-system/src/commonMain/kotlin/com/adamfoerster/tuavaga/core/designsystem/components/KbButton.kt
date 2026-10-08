package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adamfoerster.tuavaga.core.designsystem.ChamferShape
import com.adamfoerster.tuavaga.core.designsystem.KerbShapes
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

enum class KbButtonVariant { Primary, Ghost, Volt }

enum class KbButtonSize(
    internal val height: Dp,
    internal val shape: ChamferShape,
    internal val horizontalPadding: Dp,
    internal val fontSize: TextUnit,
) {
    Small(32.dp, KerbShapes.chamferSm, 16.dp, 14.sp),
    Medium(44.dp, KerbShapes.chamferMd, 24.dp, 18.sp),
    Large(56.dp, KerbShapes.chamferLg, 32.dp, 24.sp),
}

/**
 * Kerb button (`kb-btn`): chamfered, upper-case condensed label. Give it `fillMaxWidth()` for the
 * full-width buttons of the design. While [isLoading] it shows a spinner and ignores clicks.
 */
@Composable
fun KbButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KbButtonVariant = KbButtonVariant.Primary,
    size: KbButtonSize = KbButtonSize.Medium,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val colors = KerbTheme.colors
    val (background, content) = when {
        !enabled -> colors.surfaceSunken to colors.inkMuted
        variant == KbButtonVariant.Primary -> colors.apex to colors.onApex
        variant == KbButtonVariant.Volt -> colors.volt to colors.onVolt
        else -> colors.surfaceRaised to colors.ink
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(size.height)
            .graphicsLayer { translationX = if (pressed) 2.dp.toPx() else 0f }
            .clip(size.shape)
            .background(background)
            .then(
                if (enabled && variant == KbButtonVariant.Ghost) {
                    Modifier.border(2.dp, colors.lineStrong, size.shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = size.horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = content,
                strokeWidth = 2.dp,
            )
        } else {
            // Long labels on narrow phones shrink instead of being clipped.
            BasicText(
                text = text.uppercase(),
                style = KerbTheme.typography.sm.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = size.fontSize,
                    lineHeight = size.fontSize,
                    letterSpacing = 0.06.em,
                    color = content,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = size.fontSize),
            )
        }
    }
}
