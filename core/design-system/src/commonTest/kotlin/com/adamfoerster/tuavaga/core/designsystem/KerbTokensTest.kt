package com.adamfoerster.tuavaga.core.designsystem

import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.designsystem.components.initialsOf
import com.adamfoerster.tuavaga.core.designsystem.components.meterLitSegments
import com.adamfoerster.tuavaga.core.designsystem.components.tagColors
import kotlin.test.Test
import kotlin.test.assertEquals

class KerbTokensTest {

    @Test
    fun tagTonesFollowTheDesign() {
        val c = KerbColors.Dark
        assertEquals(c.volt to c.onVolt, KbTone.Go.tagColors(c))
        assertEquals(c.telemetry to c.surface, KbTone.Info.tagColors(c))
        assertEquals(c.caution to c.onCaution, KbTone.Caution.tagColors(c))
        assertEquals(c.danger to c.onDanger, KbTone.Danger.tagColors(c))
        assertEquals(c.surfaceSunken to c.ink, KbTone.Neutral.tagColors(c))
    }

    @Test
    fun lightThemeKeepsDangerReadable() {
        // Light danger uses white text; dark danger uses the surface color.
        assertEquals(KerbColors.Light.onDanger, KbTone.Danger.tagColors(KerbColors.Light).second)
        assertEquals(KerbColors.Dark.onDanger, KbTone.Danger.tagColors(KerbColors.Dark).second)
    }

    @Test
    fun meterRoundsLikeTheDesign() {
        assertEquals(1, meterLitSegments(0.3334f, 3)) // "Passo 1 de 3"
        assertEquals(2, meterLitSegments(0.6667f, 3))
        assertEquals(6, meterLitSegments(0.25f, 24)) // "Começa em"
        assertEquals(0, meterLitSegments(-1f, 24))
        assertEquals(24, meterLitSegments(2f, 24))
    }

    @Test
    fun initialsUseFirstAndLastName() {
        assertEquals("MR", initialsOf("Marina Ribeiro"))
        assertEquals("AF", initialsOf("  Adam  da Silva Foerster "))
        assertEquals("AD", initialsOf("adam"))
        assertEquals("?", initialsOf(" "))
    }
}
