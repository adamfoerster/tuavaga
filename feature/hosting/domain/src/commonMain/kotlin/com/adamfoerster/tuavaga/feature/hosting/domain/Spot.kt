package com.adamfoerster.tuavaga.feature.hosting.domain

import com.adamfoerster.tuavaga.core.domain.spot.ApprovalMode
import com.adamfoerster.tuavaga.core.domain.spot.Availability
import com.adamfoerster.tuavaga.core.domain.spot.Prices
import com.adamfoerster.tuavaga.core.domain.spot.SpotFeature
import com.adamfoerster.tuavaga.core.domain.spot.spotCode
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Error
import com.adamfoerster.tuavaga.core.domain.util.Result

enum class SpotStatus { ACTIVE, PAUSED }

/** Allowed minimum rental periods (board 14). */
val MIN_PERIOD_OPTIONS = listOf(60, 120, 240, 1440)

/** Allowed free-cancellation notices, in hours (board 14). */
val CANCEL_NOTICE_OPTIONS = listOf(2, 24, 48)

/** Rules offered as chips in "Regras da vaga". */
val PRESET_RULES = listOf("Sem caminhonete", "Altura máx. 1,90 m", "Respeitar horário", "Sem moto", "Sem lavar o carro")

/** A spot owned by the user, as listed in "Minhas vagas" and edited in the wizard. */
data class Spot(
    val id: String,
    val condoId: String,
    val levelId: String,
    val levelName: String,
    val sectorId: String?,
    val sectorName: String?,
    val number: String,
    val sizeLabel: String?,
    val description: String?,
    val features: Set<SpotFeature>,
    /** Ceiling height ("pé-direito"), in cm. */
    val heightCm: Int?,
    /** "Como chegar", shown in the spot detail. */
    val directions: String?,
    val prices: Prices,
    val minPeriodMinutes: Int,
    val cancelNoticeHours: Int,
    val approval: ApprovalMode,
    val rules: List<String>,
    val status: SpotStatus,
    val availability: Availability,
) {
    val code: String get() = spotCode(levelName, sectorName, number)
}

/** What the wizard saves; [id] null creates a new spot. */
data class SpotDraft(
    val id: String?,
    val condoId: String,
    val levelId: String,
    val sectorId: String?,
    val number: String,
    val sizeLabel: String?,
    val description: String?,
    val features: Set<SpotFeature>,
    /** Ceiling height ("pé-direito"), in cm. */
    val heightCm: Int?,
    /** "Como chegar", shown in the spot detail. */
    val directions: String?,
    val prices: Prices,
    val minPeriodMinutes: Int,
    val cancelNoticeHours: Int,
    val approval: ApprovalMode,
    val rules: List<String>,
    val availability: Availability,
)

sealed interface SpotError : Error {
    /** Another spot already uses this number on the same level and sector. */
    data object DuplicateNumber : SpotError
    data class Remote(val error: DataError.Remote) : SpotError
}

interface HostingRepository {
    /** Every spot of the user, in every condominium. */
    suspend fun mySpots(): Result<List<Spot>, DataError.Remote>

    suspend fun spot(id: String): Result<Spot, DataError.Remote>

    /** Creates or updates; returns the spot id. */
    suspend fun save(draft: SpotDraft): Result<String, SpotError>

    suspend fun setStatus(spotId: String, status: SpotStatus): EmptyResult<DataError.Remote>
}
