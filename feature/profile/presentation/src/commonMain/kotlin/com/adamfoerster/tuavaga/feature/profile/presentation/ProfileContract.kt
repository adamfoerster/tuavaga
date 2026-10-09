package com.adamfoerster.tuavaga.feature.profile.presentation

import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import com.adamfoerster.tuavaga.core.presentation.UiText

/** The vehicle sheet: [id] `null` adds a vehicle, otherwise edits it. */
data class VehicleDraft(
    val id: String? = null,
    val plate: String = "",
    val model: String = "",
    val color: String? = null,
    val type: VehicleType = VehicleType.CAR,
    val plateError: UiText? = null,
    val modelError: UiText? = null,
    val colorError: UiText? = null,
    /** Asking "Remover este veículo?". */
    val isConfirmingRemove: Boolean = false,
) {
    companion object {
        fun of(vehicle: Vehicle) = VehicleDraft(vehicle.id, vehicle.plate, vehicle.model, vehicle.color, vehicle.type)
    }
}

data class ProfileState(
    val userName: String? = null,
    val userEmail: String = "",
    val memberships: List<Membership> = emptyList(),
    val activeCondoId: String? = null,
    /** Bookings made as renter (confirmed, ongoing or completed): the "Reservas" readout. */
    val bookingCount: Int = 0,
    val vehicles: List<Vehicle> = emptyList(),
    val isLoadingVehicles: Boolean = true,
    val vehicleDraft: VehicleDraft? = null,
    /** Condominium asking "Sair deste condomínio?". */
    val leavingCondoId: String? = null,
    val isConfirmingDelete: Boolean = false,
    /** A save, removal, leave or deletion is running. */
    val isWorking: Boolean = false,
    val error: UiText? = null,
    val isSigningOut: Boolean = false,
) {
    val active: Membership? get() = memberships.firstOrNull { it.condo.id == activeCondoId } ?: memberships.firstOrNull()
}

sealed interface ProfileAction {
    data object OnSignOutClick : ProfileAction
    data object OnRetryVehicles : ProfileAction
    data object OnErrorDismiss : ProfileAction

    data object OnAddVehicleClick : ProfileAction
    data class OnVehicleClick(val vehicleId: String) : ProfileAction
    data class OnPlateChange(val value: String) : ProfileAction
    data class OnModelChange(val value: String) : ProfileAction
    data class OnColorSelect(val value: String) : ProfileAction
    data class OnTypeSelect(val value: VehicleType) : ProfileAction
    data object OnSaveVehicle : ProfileAction
    data object OnRemoveVehicleClick : ProfileAction
    data object OnRemoveVehicleConfirm : ProfileAction
    data object OnCloseVehicleSheet : ProfileAction

    data class OnCondoSelect(val condoId: String) : ProfileAction
    data class OnLeaveClick(val condoId: String) : ProfileAction
    data object OnLeaveConfirm : ProfileAction
    data object OnLeaveDismiss : ProfileAction

    data object OnDeleteAccountClick : ProfileAction
    data object OnDeleteDismiss : ProfileAction
    data object OnDeleteConfirm : ProfileAction

    // Navigation, handled by the Root.
    data object OnAddCondoClick : ProfileAction
}
