package com.adamfoerster.tuavaga.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.adamfoerster.tuavaga.core.designsystem.KerbTheme

private val FieldShape = RoundedCornerShape(2.dp)

/** The telemetry focus ring drawn 2 dp outside the box (CSS `outline: 2px; outline-offset: 2px`). */
internal fun Modifier.focusRing(focused: Boolean, color: Color): Modifier = drawWithContent {
    drawContent()
    if (focused) {
        val gap = 4.dp.toPx()
        val stroke = 2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(-gap + stroke / 2, -gap + stroke / 2),
            size = Size(size.width + 2 * gap - stroke, size.height + 2 * gap - stroke),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(stroke),
        )
    }
}

/** Upper-case label above a field or select (CSS `kb-field__label`). */
@Composable
internal fun KbFieldLabel(text: String) {
    KbText(text, KerbTheme.typography.label, color = KerbTheme.colors.inkMuted)
}

/** Message under a field: the error (prefixed "ERRO ·") or a hint. */
@Composable
internal fun KbFieldMessage(error: String?, hint: String?) {
    val colors = KerbTheme.colors
    when {
        error != null -> KbText(
            text = "ERRO · $error",
            style = KerbTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.danger,
        )
        hint != null -> KbText(hint, KerbTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

/**
 * Kerb text field (`kb-field`): label, sunken mono input with a strong border, optional [unit]
 * suffix and a message line ([error] wins over [hint]).
 */
@Composable
fun KbField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    hint: String? = null,
    unit: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = KerbTheme.colors
    val typography = KerbTheme.typography
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        error != null -> colors.danger
        focused -> colors.telemetry
        else -> colors.lineStrong
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KbFieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            singleLine = singleLine,
            textStyle = typography.data.copy(color = if (enabled) colors.ink else colors.inkMuted),
            cursorBrush = SolidColor(colors.telemetry),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            visualTransformation = visualTransformation,
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRing(focused && error == null, colors.telemetry)
                        .background(colors.surfaceSunken, FieldShape)
                        .border(2.dp, borderColor, FieldShape)
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            KbText(placeholder, typography.data, color = colors.inkMuted, maxLines = 1)
                        }
                        inner()
                    }
                    if (unit != null) {
                        KbText(
                            text = unit,
                            style = typography.dataSmall,
                            color = colors.inkMuted,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    trailing?.invoke()
                }
            },
        )
        KbFieldMessage(error, hint)
    }
}

/** [KbField] for passwords, with a "Mostrar / Ocultar" toggle. */
@Composable
fun KbPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    KbField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        error = error,
        hint = hint,
        enabled = enabled,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            KbText(
                text = if (visible) "Ocultar" else "Mostrar",
                style = KerbTheme.typography.label,
                color = KerbTheme.colors.inkMuted,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(role = Role.Button) { visible = !visible },
            )
        },
    )
}
