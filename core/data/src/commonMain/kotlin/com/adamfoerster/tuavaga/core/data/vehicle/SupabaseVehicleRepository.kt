package com.adamfoerster.tuavaga.core.data.vehicle

import com.adamfoerster.tuavaga.core.data.util.remoteCall
import com.adamfoerster.tuavaga.core.data.util.toRemoteError
import com.adamfoerster.tuavaga.core.domain.util.DataError
import com.adamfoerster.tuavaga.core.domain.util.EmptyResult
import com.adamfoerster.tuavaga.core.domain.util.Result
import com.adamfoerster.tuavaga.core.domain.vehicle.NewVehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.Vehicle
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleRepository
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
internal data class VehicleDto(
    val id: String,
    val plate: String,
    val model: String,
    val color: String,
    val type: String,
)

@Serializable
internal data class NewVehicleDto(
    val plate: String,
    val model: String,
    val color: String,
    val type: String,
)

internal fun VehicleType.toDb(): String = when (this) {
    VehicleType.CAR -> "carro"
    VehicleType.MOTORCYCLE -> "moto"
    VehicleType.LARGE -> "grande"
}

internal fun vehicleTypeFromDb(value: String): VehicleType = when (value) {
    "moto" -> VehicleType.MOTORCYCLE
    "grande" -> VehicleType.LARGE
    else -> VehicleType.CAR
}

internal fun VehicleDto.toVehicle() = Vehicle(id, plate, model, color, vehicleTypeFromDb(type))

/** Vehicles belong to the user (not to a condominium); RLS limits every query to the owner. */
internal class SupabaseVehicleRepository(
    private val postgrest: Postgrest,
) : VehicleRepository {

    override suspend fun list(): Result<List<Vehicle>, DataError.Remote> = remoteCall {
        postgrest.from("vehicles")
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<VehicleDto>()
            .map { it.toVehicle() }
    }

    override suspend fun add(vehicle: NewVehicle): Result<Vehicle, VehicleError> = try {
        val dto = NewVehicleDto(
            plate = vehicle.plate,
            model = vehicle.model.trim(),
            color = vehicle.color.trim(),
            type = vehicle.type.toDb(),
        )
        Result.Success(postgrest.from("vehicles").insert(dto) { select() }.decodeSingle<VehicleDto>().toVehicle())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.Failure(e.toVehicleError())
    }

    override suspend fun update(vehicle: Vehicle): Result<Vehicle, VehicleError> = try {
        val dto = NewVehicleDto(
            plate = vehicle.plate,
            model = vehicle.model.trim(),
            color = vehicle.color.trim(),
            type = vehicle.type.toDb(),
        )
        Result.Success(
            postgrest.from("vehicles")
                .update(dto) {
                    select()
                    filter { eq("id", vehicle.id) }
                }
                .decodeSingle<VehicleDto>()
                .toVehicle(),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.Failure(e.toVehicleError())
    }

    override suspend fun remove(vehicleId: String): EmptyResult<VehicleError> = try {
        postgrest.from("vehicles").delete { filter { eq("id", vehicleId) } }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.Failure(e.toVehicleError())
    }
}

internal fun Throwable.toVehicleError(): VehicleError =
    (this as? PostgrestRestException)?.let { vehicleErrorOf(it.code, it.message) } ?: VehicleError.Remote(toRemoteError())

/** 23505 = unique_violation on (owner_id, plate); vehicle_in_use comes from the delete guard trigger. */
internal fun vehicleErrorOf(code: String?, message: String?): VehicleError? = when {
    code == "23505" -> VehicleError.DuplicatePlate
    message?.contains("vehicle_in_use") == true -> VehicleError.InUse
    else -> null
}
