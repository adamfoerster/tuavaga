package com.adamfoerster.tuavaga.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalKerbColors = staticCompositionLocalOf { KerbColors.Dark }
private val LocalKerbTypography = staticCompositionLocalOf<KerbTypography> {
    error("KerbTypography not provided; wrap the UI in TuaVagaTheme")
}

/** The current Kerb tokens inside [TuaVagaTheme]. */
object KerbTheme {
    val colors: KerbColors
        @Composable @ReadOnlyComposable get() = LocalKerbColors.current
    val typography: KerbTypography
        @Composable @ReadOnlyComposable get() = LocalKerbTypography.current
}

/**
 * App theme: the Kerb design system, dark by default and light when the system asks for it.
 * MaterialTheme is filled from the same tokens so the few Material widgets used inside Kb*
 * components (menus, progress) stay on-palette.
 */
@Composable
fun TuaVagaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) KerbColors.Dark else KerbColors.Light
    val fonts = kerbFonts()
    val typography = remember(fonts.display, fonts.body, fonts.mono) { kerbTypography(fonts) }
    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val material = scheme.copy(
        primary = colors.apex,
        onPrimary = colors.onApex,
        background = colors.surface,
        onBackground = colors.ink,
        surface = colors.surfaceRaised,
        onSurface = colors.ink,
        surfaceVariant = colors.surfaceSunken,
        onSurfaceVariant = colors.inkMuted,
        outline = colors.lineStrong,
        error = colors.danger,
        onError = colors.onDanger,
    )
    CompositionLocalProvider(
        LocalKerbColors provides colors,
        LocalKerbTypography provides typography,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.telemetry,
            backgroundColor = colors.telemetry.copy(alpha = 0.4f),
        ),
    ) {
        MaterialTheme(colorScheme = material, content = content)
    }
}
