package com.adamfoerster.tuavaga.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Kerb color tokens (values from the design's tokens.json). */
@Immutable
data class KerbColors(
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkMuted: Color,
    val apex: Color,
    val onApex: Color,
    val apexText: Color,
    val danger: Color,
    val onDanger: Color,
    val volt: Color,
    val onVolt: Color,
    val voltText: Color,
    val telemetry: Color,
    val caution: Color,
    val onCaution: Color,
    val cautionText: Color,
    val isDark: Boolean,
) {
    companion object {
        val Dark = KerbColors(
            surface = Color(0xFF14110E),
            surfaceRaised = Color(0xFF1F1B17),
            surfaceSunken = Color(0xFF2A2520),
            line = Color(0xFF3A342D),
            lineStrong = Color(0xFF8D8372),
            ink = Color(0xFFF3EAD8),
            inkMuted = Color(0xFFB3A791),
            apex = Color(0xFFF4B91A),
            onApex = Color(0xFF14110E),
            apexText = Color(0xFFF4B91A),
            danger = Color(0xFFFF6150),
            onDanger = Color(0xFF14110E),
            volt = Color(0xFF7CCF4F),
            onVolt = Color(0xFF14110E),
            voltText = Color(0xFF8FE05F),
            telemetry = Color(0xFF74C6D6),
            caution = Color(0xFFFF8A1F),
            onCaution = Color(0xFF14110E),
            cautionText = Color(0xFFFF9A3C),
            isDark = true,
        )

        val Light = KerbColors(
            surface = Color(0xFFECE4D2),
            surfaceRaised = Color(0xFFF8F3E8),
            surfaceSunken = Color(0xFFDDD3BC),
            line = Color(0xFFC9BDA3),
            lineStrong = Color(0xFF75694F),
            ink = Color(0xFF17130E),
            inkMuted = Color(0xFF53493A),
            apex = Color(0xFFF4B91A),
            onApex = Color(0xFF14110E),
            apexText = Color(0xFF7A5400),
            danger = Color(0xFFB3201A),
            onDanger = Color(0xFFFFFFFF),
            volt = Color(0xFF7CCF4F),
            onVolt = Color(0xFF14110E),
            voltText = Color(0xFF2F6B12),
            telemetry = Color(0xFF005F78),
            caution = Color(0xFFFF8A1F),
            onCaution = Color(0xFF14110E),
            cautionText = Color(0xFF8A4200),
            isDark = false,
        )
    }
}
