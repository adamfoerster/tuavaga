package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo

import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.NewCondominium

/** Which condominium the "Seus dados" step will join. */
sealed interface CondoTarget {
    /** Found by search or invite code. */
    data class Existing(val preview: CondoPreview) : CondoTarget

    /** Typed in "Cadastrar meu condomínio"; created together with the membership. */
    data class New(val condo: NewCondominium) : CondoTarget
}

/**
 * State shared by the steps of the condominium flow (one flow at a time, so a Koin single).
 * [createdCondoId] remembers a condominium already created when a later part of the submit
 * failed, so retrying joins it instead of creating a duplicate.
 */
class CondoOnboardingSession {
    var target: CondoTarget? = null
        set(value) {
            field = value
            createdCondoId = null
        }

    var createdCondoId: String? = null

    fun clear() {
        target = null
    }
}
