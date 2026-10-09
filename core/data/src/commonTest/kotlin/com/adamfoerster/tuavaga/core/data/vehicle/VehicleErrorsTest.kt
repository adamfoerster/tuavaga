package com.adamfoerster.tuavaga.core.data.vehicle

import com.adamfoerster.tuavaga.core.data.condo.leaveErrorOf
import com.adamfoerster.tuavaga.core.domain.condo.CondoError
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VehicleErrorsTest {

    @Test
    fun vehicleErrors() {
        assertEquals(VehicleError.DuplicatePlate, vehicleErrorOf("23505", "duplicate key"))
        assertEquals(VehicleError.InUse, vehicleErrorOf("P0001", "vehicle_in_use"))
        assertNull(vehicleErrorOf("500", "boom"))
    }

    @Test
    fun leaveErrors() {
        assertEquals(CondoError.ActiveBookings, leaveErrorOf("active_bookings"))
        assertNull(leaveErrorOf(null))
    }
}
