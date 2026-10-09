package com.adamfoerster.tuavaga.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.booking.BookingRepository
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.validation.BrFormats
import com.adamfoerster.tuavaga.core.domain.vehicle.NewVehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.profile.domain.AccountError
import com.adamfoerster.tuavaga.feature.profile.domain.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Board 24 · Perfil: identity, vehicles, condominiums, sign out and account deletion. */
class ProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val condoRepository: CondoRepository,
    private val activeCondoRepository: ActiveCondoRepository,
    private val vehicleRepository: VehicleRepository,
    bookingRepository: BookingRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        sessionRepository.sessionState
            .filterIsInstance<SessionState.SignedIn>()
            .onEach { session -> _state.update { it.copy(userName = session.user.fullName, userEmail = session.user.email) } }
            .launchIn(viewModelScope)
        condoRepository.memberships.onEach { list -> _state.update { it.copy(memberships = list) } }.launchIn(viewModelScope)
        activeCondoRepository.activeCondoId.onEach { id -> _state.update { it.copy(activeCondoId = id) } }.launchIn(viewModelScope)
        bookingRepository.bookings
            .onEach { all -> _state.update { it.copy(bookingCount = all.count { b -> b.role == BookingRole.RENTER && b.status in COUNTED }) } }
            .launchIn(viewModelScope)
        loadVehicles()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.OnSignOutClick -> signOut()
            ProfileAction.OnRetryVehicles -> loadVehicles()
            ProfileAction.OnErrorDismiss -> _state.update { it.copy(error = null) }

            ProfileAction.OnAddVehicleClick -> _state.update { it.copy(vehicleDraft = VehicleDraft(), error = null) }
            is ProfileAction.OnVehicleClick -> _state.value.vehicles.firstOrNull { it.id == action.vehicleId }?.let { v ->
                _state.update { it.copy(vehicleDraft = VehicleDraft.of(v), error = null) }
            }
            is ProfileAction.OnPlateChange -> draft { it.copy(plate = action.value, plateError = null) }
            is ProfileAction.OnModelChange -> draft { it.copy(model = action.value, modelError = null) }
            is ProfileAction.OnColorSelect -> draft { it.copy(color = action.value, colorError = null) }
            is ProfileAction.OnTypeSelect -> draft { it.copy(type = action.value) }
            ProfileAction.OnSaveVehicle -> saveVehicle()
            ProfileAction.OnRemoveVehicleClick -> draft { it.copy(isConfirmingRemove = true) }
            ProfileAction.OnRemoveVehicleConfirm -> removeVehicle()
            ProfileAction.OnCloseVehicleSheet -> _state.update { it.copy(vehicleDraft = null, error = null) }

            is ProfileAction.OnCondoSelect -> viewModelScope.launch { activeCondoRepository.setActiveCondo(action.condoId) }
            is ProfileAction.OnLeaveClick -> _state.update { it.copy(leavingCondoId = action.condoId, error = null) }
            ProfileAction.OnLeaveDismiss -> _state.update { it.copy(leavingCondoId = null) }
            ProfileAction.OnLeaveConfirm -> leaveCondo()

            ProfileAction.OnDeleteAccountClick -> _state.update { it.copy(isConfirmingDelete = true, error = null) }
            ProfileAction.OnDeleteDismiss -> _state.update { it.copy(isConfirmingDelete = false) }
            ProfileAction.OnDeleteConfirm -> deleteAccount()

            ProfileAction.OnAddCondoClick -> Unit
        }
    }

    private fun draft(change: (VehicleDraft) -> VehicleDraft) {
        _state.update { state -> state.copy(vehicleDraft = state.vehicleDraft?.let(change)) }
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingVehicles = true) }
            when (val result = vehicleRepository.list()) {
                is Result.Success -> _state.update { it.copy(isLoadingVehicles = false, vehicles = result.data) }
                is Result.Failure -> _state.update { it.copy(isLoadingVehicles = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun saveVehicle() {
        val draft = _state.value.vehicleDraft ?: return
        if (_state.value.isWorking) return
        val plate = BrFormats.normalizePlate(draft.plate)
        val model = draft.model.trim()
        val color = draft.color
        val checked = draft.copy(
            plateError = ProfileTexts.invalidPlate.takeIf { plate == null },
            modelError = ProfileTexts.modelRequired.takeIf { model.isEmpty() },
            colorError = ProfileTexts.colorRequired.takeIf { color == null },
        )
        if (plate == null || model.isEmpty() || color == null) {
            _state.update { it.copy(vehicleDraft = checked) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            val result = if (draft.id == null) {
                vehicleRepository.add(NewVehicle(plate, model, color, draft.type))
            } else {
                vehicleRepository.update(Vehicle(draft.id, plate, model, color, draft.type))
            }
            when (result) {
                is Result.Success -> _state.update { state ->
                    val saved = result.data
                    val list = if (state.vehicles.any { it.id == saved.id }) {
                        state.vehicles.map { if (it.id == saved.id) saved else it }
                    } else {
                        state.vehicles + saved
                    }
                    state.copy(isWorking = false, vehicles = list, vehicleDraft = null)
                }
                is Result.Failure -> _state.update { state ->
                    when (result.error) {
                        VehicleError.DuplicatePlate ->
                            state.copy(isWorking = false, vehicleDraft = state.vehicleDraft?.copy(plateError = ProfileTexts.duplicatePlate))
                        else -> state.copy(isWorking = false, error = result.error.toText())
                    }
                }
            }
        }
    }

    private fun removeVehicle() {
        val id = _state.value.vehicleDraft?.id ?: return
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            when (val result = vehicleRepository.remove(id)) {
                is Result.Success -> _state.update { state ->
                    state.copy(isWorking = false, vehicles = state.vehicles.filterNot { it.id == id }, vehicleDraft = null)
                }
                is Result.Failure -> _state.update { state ->
                    state.copy(
                        isWorking = false,
                        error = result.error.toText(),
                        vehicleDraft = state.vehicleDraft?.copy(isConfirmingRemove = false),
                    )
                }
            }
        }
    }

    private fun leaveCondo() {
        val condoId = _state.value.leavingCondoId ?: return
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            val result = condoRepository.leave(condoId)
            _state.update {
                when (result) {
                    is Result.Success -> it.copy(isWorking = false, leavingCondoId = null)
                    is Result.Failure -> it.copy(
                        isWorking = false,
                        leavingCondoId = null,
                        error = when (val error = result.error) {
                            CondoError.ActiveBookings -> ProfileTexts.leaveWithBookings
                            is CondoError.Remote -> error.error.toUiText()
                            CondoError.InvalidBlock -> null
                        },
                    )
                }
            }
        }
    }

    private fun deleteAccount() {
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            when (val result = accountRepository.deleteAccount()) {
                // The server account is gone; clearing the local session moves the app to the login.
                is Result.Success -> sessionRepository.signOut()
                is Result.Failure -> _state.update {
                    it.copy(
                        isWorking = false,
                        isConfirmingDelete = false,
                        error = when (val error = result.error) {
                            AccountError.BookingInProgress -> ProfileTexts.deleteWhileParked
                            is AccountError.Remote -> error.error.toUiText()
                        },
                    )
                }
            }
        }
    }

    private fun signOut() {
        if (_state.value.isSigningOut) return
        viewModelScope.launch {
            _state.update { it.copy(isSigningOut = true) }
            // The local session is always cleared, so the app root moves to the login screen
            // even when the server could not be reached; nothing else to handle here.
            sessionRepository.signOut()
        }
    }

    private fun VehicleError.toText(): UiText = when (this) {
        VehicleError.DuplicatePlate -> ProfileTexts.duplicatePlate
        VehicleError.InUse -> ProfileTexts.vehicleInUse
        is VehicleError.Remote -> error.toUiText()
    }

    private companion object {
        val COUNTED = setOf(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)
    }
}

internal object ProfileTexts {
    val invalidPlate = UiText.Dynamic("Placa inválida. Ex.: ABC1D23 ou ABC1234.")
    val modelRequired = UiText.Dynamic("Informe o modelo.")
    val colorRequired = UiText.Dynamic("Escolha a cor.")
    val duplicatePlate = UiText.Dynamic("Você já cadastrou esta placa.")
    val vehicleInUse = UiText.Dynamic("Este veículo está numa reserva pendente ou ativa. Remova depois que ela terminar.")
    val leaveWithBookings = UiText.Dynamic("Você tem reservas pendentes ou ativas neste condomínio. Cancele ou conclua antes de sair.")
    val deleteWhileParked = UiText.Dynamic("Há uma reserva em andamento. Faça o check-out antes de excluir a conta.")
}
