package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.resident

import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.core.presentation.UiText

data class VehicleDraft(
    val plate: String = "",
    val model: String = "",
    val color: String? = null,
    val type: VehicleType = VehicleType.CAR,
    val plateError: UiText? = null,
    val modelError: UiText? = null,
    val colorError: UiText? = null,
) {
    val isBlank: Boolean get() = plate.isBlank() && model.isBlank() && color == null
}

data class ResidentDataState(
    val condoName: String = "",
    val isNewCondo: Boolean = false,
    /** Blocks offered by the condominium; empty = free of blocks (no block field). */
    val blocks: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val fullName: String = "",
    val block: String? = null,
    val unit: String = "",
    val phone: String = "",
    val kind: MembershipKind = MembershipKind.RESIDENT,
    val existingVehicles: List<Vehicle> = emptyList(),
    val vehicleDrafts: List<VehicleDraft> = emptyList(),
    val termsAccepted: Boolean = false,
    val nameError: UiText? = null,
    val blockError: UiText? = null,
    val unitError: UiText? = null,
    val phoneError: UiText? = null,
    val termsError: UiText? = null,
    val isSubmitting: Boolean = false,
    val error: UiText? = null,
)

sealed interface ResidentDataAction {
    data class OnFullNameChange(val value: String) : ResidentDataAction
    data class OnBlockSelect(val block: String) : ResidentDataAction
    data class OnUnitChange(val value: String) : ResidentDataAction
    data class OnPhoneChange(val value: String) : ResidentDataAction
    data class OnKindSelect(val kind: MembershipKind) : ResidentDataAction
    data object OnAddVehicle : ResidentDataAction
    data class OnRemoveVehicle(val index: Int) : ResidentDataAction
    data class OnPlateChange(val index: Int, val value: String) : ResidentDataAction
    data class OnModelChange(val index: Int, val value: String) : ResidentDataAction
    data class OnColorSelect(val index: Int, val color: String) : ResidentDataAction
    data class OnTypeSelect(val index: Int, val type: VehicleType) : ResidentDataAction
    data class OnTermsChange(val accepted: Boolean) : ResidentDataAction
    data object OnSubmit : ResidentDataAction
    data object OnRetryLoad : ResidentDataAction
    data object OnBackClick : ResidentDataAction
}

sealed interface ResidentDataEvent {
    /** Joined (and the condominium is now the active one). */
    data object Finished : ResidentDataEvent
}

/** Colors offered in the vehicle form (stored as typed). */
val VEHICLE_COLORS = listOf("Preto", "Branco", "Prata", "Cinza", "Vermelho", "Azul", "Outra")
