package com.adamfoerster.tuavaga.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adamfoerster.tuavaga.core.designsystem.resources.Res
import com.adamfoerster.tuavaga.core.designsystem.resources.barlow_bold
import com.adamfoerster.tuavaga.core.designsystem.resources.barlow_condensed_bold_italic
import com.adamfoerster.tuavaga.core.designsystem.resources.barlow_condensed_extrabold_italic
import com.adamfoerster.tuavaga.core.designsystem.resources.barlow_medium
import com.adamfoerster.tuavaga.core.designsystem.resources.barlow_regular
import com.adamfoerster.tuavaga.core.designsystem.resources.barlow_semibold
import com.adamfoerster.tuavaga.core.designsystem.resources.jetbrains_mono_bold
import com.adamfoerster.tuavaga.core.designsystem.resources.jetbrains_mono_medium
import org.jetbrains.compose.resources.Font

/**
 * Kerb type scale (CSS classes `t-xl` … `t-ds`). Display styles (`xl`…`sm`) and [label] are shown
 * in upper case; Compose has no text-transform, so [KbText] and the Kb* components do it.
 */
@Immutable
data class KerbTypography(
    val xl: TextStyle,
    val lg: TextStyle,
    val md: TextStyle,
    val sm: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val dataXl: TextStyle,
    val data: TextStyle,
    val dataSmall: TextStyle,
) {
    /** Styles the design always renders in upper case. */
    fun isUppercase(style: TextStyle): Boolean =
        style == xl || style == lg || style == md || style == sm || style == label
}

@Immutable
class KerbFonts(val display: FontFamily, val body: FontFamily, val mono: FontFamily)

@Composable
internal fun kerbFonts(): KerbFonts = KerbFonts(
    display = FontFamily(
        Font(Res.font.barlow_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic),
        Font(Res.font.barlow_condensed_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    ),
    body = FontFamily(
        Font(Res.font.barlow_regular, FontWeight.Normal),
        Font(Res.font.barlow_medium, FontWeight.Medium),
        Font(Res.font.barlow_semibold, FontWeight.SemiBold),
        Font(Res.font.barlow_bold, FontWeight.Bold),
    ),
    mono = FontFamily(
        Font(Res.font.jetbrains_mono_medium, FontWeight.Medium),
        Font(Res.font.jetbrains_mono_bold, FontWeight.Bold),
    ),
)

internal fun kerbTypography(fonts: KerbFonts): KerbTypography {
    fun style(family: FontFamily, weight: FontWeight, size: Int, line: Int, spacing: Double, italic: Boolean = false) =
        TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            fontSize = size.sp,
            lineHeight = line.sp,
            letterSpacing = spacing.em,
        )
    return KerbTypography(
        xl = style(fonts.display, FontWeight.ExtraBold, 72, 66, 0.01, italic = true),
        lg = style(fonts.display, FontWeight.ExtraBold, 48, 46, 0.01, italic = true),
        md = style(fonts.display, FontWeight.Bold, 32, 32, 0.02, italic = true),
        sm = style(fonts.display, FontWeight.Bold, 22, 24, 0.04, italic = true),
        bodyLarge = style(fonts.body, FontWeight.Normal, 18, 28, 0.0),
        body = style(fonts.body, FontWeight.Normal, 15, 22, 0.0),
        bodySmall = style(fonts.body, FontWeight.Normal, 13, 18, 0.0),
        label = style(fonts.body, FontWeight.SemiBold, 12, 16, 0.14),
        dataXl = style(fonts.mono, FontWeight.Bold, 44, 44, -0.02),
        data = style(fonts.mono, FontWeight.Medium, 16, 24, 0.0),
        dataSmall = style(fonts.mono, FontWeight.Medium, 12, 16, 0.0),
    )
}
