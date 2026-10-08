package com.adamfoerster.tuavaga.core.domain.vehicle

import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.Error
import com.adamfoerster.tuavaga.core.domain.util.Result

enum class VehicleType { CAR, MOTORCYCLE, LARGE }

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
    data class Remote(val error: DataError.Remote) : VehicleError
}

interface VehicleRepository {
    suspend fun list(): Result<List<Vehicle>, DataError.Remote>

    suspend fun add(vehicle: NewVehicle): Result<Vehicle, VehicleError>
}
