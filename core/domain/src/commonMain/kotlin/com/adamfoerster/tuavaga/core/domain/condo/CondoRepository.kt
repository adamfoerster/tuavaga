package com.adamfoerster.tuavaga.core.domain.condo

import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Error
import com.adamfoerster.tuavaga.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

sealed interface CondoError : Error {
    /** The chosen block is not one of the condominium's blocks. */
    data object InvalidBlock : CondoError

    /** Leaving while a booking there is pending, confirmed or ongoing. */
    data object ActiveBookings : CondoError
    data class Remote(val error: DataError.Remote) : CondoError
}

/**
 * The signed-in user's condominiums. [memberships] is a local cache (works offline);
 * [refreshMemberships] reloads it from the backend.
 */
interface CondoRepository {
    val memberships: Flow<List<Membership>>

    suspend fun refreshMemberships(): EmptyResult<DataError.Remote>

    suspend fun search(query: String): Result<List<CondoPreview>, DataError.Remote>

    /** `null` when no condominium has this code. */
    suspend fun findByInviteCode(code: String): Result<CondoPreview?, DataError.Remote>

    suspend fun blocksOf(condoId: String): Result<List<String>, DataError.Remote>

    /** Levels and sectors of a condominium the user belongs to, in display order. */
    suspend fun garageOf(condoId: String): Result<List<GarageLevel>, DataError.Remote>

    /**
     * Joins (or updates the link to) [condoId]. Does not touch [memberships]: callers refresh when
     * their flow is done, since a new membership moves the app out of onboarding.
     */
    suspend fun join(condoId: String, resident: ResidentInfo): EmptyResult<CondoError>

    /** Creates the condominium with its garage and joins it; returns the new id. Same refresh rule as [join]. */
    suspend fun create(condo: NewCondominium, resident: ResidentInfo): Result<String, CondoError>

    /** Leaves [condoId] (the user's spots there are paused) and refreshes [memberships]. */
    suspend fun leave(condoId: String): EmptyResult<CondoError>
}

/** Which condominium is active on this device (everything below the selector follows it). */
interface ActiveCondoRepository {
    val activeCondoId: Flow<String?>

    suspend fun setActiveCondo(condoId: String)
}
