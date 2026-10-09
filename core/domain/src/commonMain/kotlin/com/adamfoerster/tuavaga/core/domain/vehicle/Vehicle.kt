package com.adamfoerster.tuavaga.core.domain.vehicle

import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Error
import com.adamfoerster.tuavaga.core.domain.util.Result

enum class VehicleType { CAR, MOTORCYCLE, LARGE }

/** Colors offered in the vehicle forms (stored as typed). */
val VEHICLE_COLORS = listOf("Preto", "Branco", "Prata", "Cinza", "Vermelho", "Azul", "Outra")

data class Vehicle(
    val id: String,
    /** Normalized: upper case, no hyphen (ABC1D23). */
    val plate: String,
    val model: String,
    val color: String,
    val type: VehicleType,
)

data class NewVehicle(
    val plate: String,
    val model: String,
    val color: String,
    val type: VehicleType,
)

sealed interface VehicleError : Error {
    /** The user already registered this plate. */
    data object DuplicatePlate : VehicleError

    /** Removing a vehicle of a pending, confirmed or ongoing booking. */
    data object InUse : VehicleError
    data class Remote(val error: DataError.Remote) : VehicleError
}

interface VehicleRepository {
    suspend fun list(): Result<List<Vehicle>, DataError.Remote>

    suspend fun add(vehicle: NewVehicle): Result<Vehicle, VehicleError>

    suspend fun update(vehicle: Vehicle): Result<Vehicle, VehicleError>

    suspend fun remove(vehicleId: String): EmptyResult<VehicleError>
}
