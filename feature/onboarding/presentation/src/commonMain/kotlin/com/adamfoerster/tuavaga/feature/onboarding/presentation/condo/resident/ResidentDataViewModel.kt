package com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.resident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.tuavaga.core.domain.condo.ActiveCondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.condo.CondoRepository
import com.adamfoerster.tuavaga.core.domain.condo.ResidentInfo
import com.adamfoerster.tuavaga.core.domain.session.SessionRepository
import com.adamfoerster.tuavaga.core.domain.session.SessionState
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.validation.BrFormats
import com.adamfoerster.tuavaga.core.domain.vehicle.NewVehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import com.adamfoerster.tuavaga.core.presentation.UiText
import com.adamfoerster.tuavaga.core.presentation.toUiText
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoOnboardingSession
import com.adamfoerster.tuavaga.feature.onboarding.presentation.condo.CondoTarget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Board 03 · Cadastro do morador e veículo. Submitting joins (or creates and joins) the
 * condominium from [CondoOnboardingSession], makes it the active one and saves new vehicles.
 * Vehicles are optional: a blank vehicle form is ignored.
 */
class ResidentDataViewModel(
    private val session: CondoOnboardingSession,
    private val condoRepository: CondoRepository,
    private val activeCondoRepository: ActiveCondoRepository,
    private val vehicleRepository: VehicleRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ResidentDataState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<ResidentDataEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ResidentDataAction) {
        when (action) {
            is ResidentDataAction.OnFullNameChange -> _state.update { it.copy(fullName = action.value, nameError = null) }
            is ResidentDataAction.OnBlockSelect -> _state.update { it.copy(block = action.block, blockError = null) }
            is ResidentDataAction.OnUnitChange -> _state.update { it.copy(unit = action.value, unitError = null) }
            is ResidentDataAction.OnPhoneChange -> _state.update { it.copy(phone = action.value, phoneError = null) }
            is ResidentDataAction.OnKindSelect -> _state.update { it.copy(kind = action.kind) }
            ResidentDataAction.OnAddVehicle -> _state.update { it.copy(vehicleDrafts = it.vehicleDrafts + VehicleDraft()) }
            is ResidentDataAction.OnRemoveVehicle -> _state.update {
                it.copy(vehicleDrafts = it.vehicleDrafts.filterIndexed { i, _ -> i != action.index })
            }
            is ResidentDataAction.OnPlateChange -> updateDraft(action.index) { it.copy(plate = action.value, plateError = null) }
            is ResidentDataAction.OnModelChange -> updateDraft(action.index) { it.copy(model = action.value, modelError = null) }
            is ResidentDataAction.OnColorSelect -> updateDraft(action.index) { it.copy(color = action.color, colorError = null) }
            is ResidentDataAction.OnTypeSelect -> updateDraft(action.index) { it.copy(type = action.type) }
            is ResidentDataAction.OnTermsChange -> _state.update { it.copy(termsAccepted = action.accepted, termsError = null) }
            ResidentDataAction.OnSubmit -> submit()
            ResidentDataAction.OnRetryLoad -> load()
            ResidentDataAction.OnBackClick -> Unit
        }
    }

    private fun load() {
        val target = session.target
        if (target == null) {
            _state.update { it.copy(isLoading = false, error = ResidentTexts.flowExpired) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val user = (sessionRepository.sessionState.first { it !is SessionState.Loading } as? SessionState.SignedIn)?.user
            val blocks = when (target) {
                is CondoTarget.New -> Result.Success(target.condo.blocks)
                is CondoTarget.Existing -> target.preview.blocks?.let { Result.Success(it) }
                    ?: condoRepository.blocksOf(target.preview.id)
            }
            val vehicles = vehicleRepository.list()
            val failure = (blocks as? Result.Failure)?.error ?: (vehicles as? Result.Failure)?.error
            val existing = (vehicles as? Result.Success)?.data.orEmpty()
            _state.update { current ->
                current.copy(
                    condoName = when (target) {
                        is CondoTarget.New -> target.condo.name
                        is CondoTarget.Existing -> target.preview.name
                    },
                    isNewCondo = target is CondoTarget.New,
                    blocks = (blocks as? Result.Success)?.data.orEmpty(),
                    fullName = current.fullName.ifEmpty { user?.fullName.orEmpty() },
                    existingVehicles = existing,
                    vehicleDrafts = if (current.vehicleDrafts.isEmpty() && existing.isEmpty()) listOf(VehicleDraft()) else current.vehicleDrafts,
                    isLoading = false,
                    error = failure?.toUiText(),
                )
            }
        }
    }

    private fun updateDraft(index: Int, transform: (VehicleDraft) -> VehicleDraft) = _state.update {
        it.copy(vehicleDrafts = it.vehicleDrafts.mapIndexed { i, draft -> if (i == index) transform(draft) else draft }, error = null)
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting || current.isLoading) return
        val target = session.target ?: run {
            _state.update { it.copy(error = ResidentTexts.flowExpired) }
            return
        }
        val validated = validate(current)
        _state.value = validated.state
        if (!validated.isValid) return

        val resident = ResidentInfo(
            fullName = current.fullName.trim(),
            phone = current.phone.takeIf { it.isNotBlank() }?.let { BrFormats.normalizePhone(it) },
            block = current.block.takeIf { current.blocks.isNotEmpty() },
            unit = current.unit.trim(),
            kind = current.kind,
        )

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            // Vehicles first: refreshing the memberships at the end takes the app out of
            // onboarding, which disposes this screen.
            if (!saveVehicles()) return@launch
            val condoId = when (val joined = joinOrCreate(target, resident)) {
                is Result.Success -> joined.data
                is Result.Failure -> {
                    _state.update {
                        when (val error = joined.error) {
                            CondoError.InvalidBlock -> it.copy(isSubmitting = false, blockError = ResidentTexts.blockRequired)
                            is CondoError.Remote -> it.copy(isSubmitting = false, error = error.error.toUiText())
                            // Only raised when leaving; never by join/create.
                            CondoError.ActiveBookings -> it.copy(isSubmitting = false)
                        }
                    }
                    return@launch
                }
            }
            activeCondoRepository.setActiveCondo(condoId)
            session.clear()
            // During the first onboarding the new membership switches the app to the main screens
            // right here; if the refresh fails, the root retries when it receives Finished.
            condoRepository.refreshMemberships()
            _state.update { it.copy(isSubmitting = false) }
            eventChannel.send(ResidentDataEvent.Finished)
        }
    }

    private suspend fun joinOrCreate(target: CondoTarget, resident: ResidentInfo): Result<String, CondoError> =
        when (target) {
            is CondoTarget.Existing -> when (val result = condoRepository.join(target.preview.id, resident)) {
                is Result.Success -> Result.Success(target.preview.id)
                is Result.Failure -> result
            }
            is CondoTarget.New -> {
                val alreadyCreated = session.createdCondoId
                if (alreadyCreated != null) {
                    when (val result = condoRepository.join(alreadyCreated, resident)) {
                        is Result.Success -> Result.Success(alreadyCreated)
                        is Result.Failure -> result
                    }
                } else {
                    condoRepository.create(target.condo, resident).also { result ->
                        if (result is Result.Success) session.createdCondoId = result.data
                    }
                }
            }
        }

    /** Saves the filled vehicle forms one by one; a saved one moves to [ResidentDataState.existingVehicles]. */
    private suspend fun saveVehicles(): Boolean {
        while (true) {
            val drafts = _state.value.vehicleDrafts
            val index = drafts.indexOfFirst { !it.isBlank }
            if (index < 0) break
            val draft = drafts[index]
            val vehicle = NewVehicle(
                plate = BrFormats.normalizePlate(draft.plate) ?: return false,
                model = draft.model.trim(),
                color = draft.color ?: return false,
                type = draft.type,
            )
            when (val result = vehicleRepository.add(vehicle)) {
                is Result.Success -> _state.update {
                    it.copy(
                        existingVehicles = it.existingVehicles + result.data,
                        vehicleDrafts = it.vehicleDrafts.filterIndexed { i, _ -> i != index },
                    )
                }
                is Result.Failure -> {
                    _state.update {
                        when (val error = result.error) {
                            VehicleError.DuplicatePlate -> it.copy(
                                isSubmitting = false,
                                vehicleDrafts = it.vehicleDrafts.mapIndexed { i, d ->
                                    if (i == index) d.copy(plateError = ResidentTexts.duplicatePlate) else d
                                },
                            )
                            is VehicleError.Remote -> it.copy(isSubmitting = false, error = error.error.toUiText())
                            // Only raised when removing; never by add.
                            VehicleError.InUse -> it.copy(isSubmitting = false)
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private class Validated(val state: ResidentDataState, val isValid: Boolean)

    private fun validate(s: ResidentDataState): Validated {
        val drafts = s.vehicleDrafts.map { draft ->
            if (draft.isBlank) {
                draft
            } else {
                draft.copy(
                    plateError = ResidentTexts.invalidPlate.takeIf { BrFormats.normalizePlate(draft.plate) == null },
                    modelError = ResidentTexts.modelRequired.takeIf { draft.model.isBlank() },
                    colorError = ResidentTexts.colorRequired.takeIf { draft.color == null },
                )
            }
        }
        val checked = s.copy(
            nameError = ResidentTexts.nameRequired.takeIf { s.fullName.trim().length < 2 },
            blockError = ResidentTexts.blockRequired.takeIf { s.blocks.isNotEmpty() && s.block !in s.blocks },
            unitError = ResidentTexts.unitRequired.takeIf { s.unit.isBlank() },
            phoneError = ResidentTexts.invalidPhone.takeIf { s.phone.isNotBlank() && BrFormats.normalizePhone(s.phone) == null },
            termsError = ResidentTexts.termsRequired.takeIf { !s.termsAccepted },
            vehicleDrafts = drafts,
        )
        val hasError = listOf(checked.nameError, checked.blockError, checked.unitError, checked.phoneError, checked.termsError)
            .any { it != null } ||
            drafts.any { it.plateError != null || it.modelError != null || it.colorError != null }
        return Validated(checked, !hasError)
    }
}

internal object ResidentTexts {
    val nameRequired = UiText.Dynamic("Informe seu nome.")
    val blockRequired = UiText.Dynamic("Escolha o bloco ou torre.")
    val unitRequired = UiText.Dynamic("Informe a unidade.")
    val invalidPhone = UiText.Dynamic("Telefone com DDD, ex.: (11) 91234-5678.")
    val termsRequired = UiText.Dynamic("Aceite os termos para continuar.")
    val invalidPlate = UiText.Dynamic("Placa inválida. Ex.: ABC1D23 ou ABC1234.")
    val modelRequired = UiText.Dynamic("Informe o modelo.")
    val colorRequired = UiText.Dynamic("Escolha a cor.")
    val duplicatePlate = UiText.Dynamic("Você já cadastrou esta placa.")
    val flowExpired = UiText.Dynamic("Volte e escolha o condomínio de novo.")
}
