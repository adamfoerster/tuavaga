package com.adamfoerster.tuavaga.core.data.condo

import com.adamfoerster.tuavaga.core.data.vehicle.toDb
import com.adamfoerster.tuavaga.core.data.vehicle.vehicleTypeFromDb
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CondoMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun embeddedMembershipRoundTripsThroughTheCache() {
        val dto = json.decodeFromString<MembershipDto>(
            """
            {"block":"B","unit":"142","kind":"morador",
             "condominiums":{"id":"c1","name":"Residencial Alameda Verde","address":"Rua das Figueiras, 410",
                             "cep":"01310-100","blocks":["A","B","C"],"invite_code":"AV-4K7Q"}}
            """,
        )

        val membership = dto.toEntity(userId = "u1", position = 0).toMembership()

        assertEquals("c1", membership.condo.id)
        assertEquals(listOf("A", "B", "C"), membership.condo.blocks)
        assertEquals("AV-4K7Q", membership.condo.inviteCode)
        assertEquals("B", membership.block)
        assertEquals(MembershipKind.RESIDENT, membership.kind)
    }

    @Test
    fun condoWithoutBlocksStaysEmpty() {
        val dto = MembershipDto(
            block = null,
            unit = "Sala 3-15",
            kind = "trabalho",
            condo = CondoDto("c2", "Edifício Santa Clara", "Rua X, 1", null, emptyList(), "SC-2B9K"),
        )

        val membership = dto.toEntity("u1", 1).toMembership()

        assertEquals(emptyList(), membership.condo.blocks)
        assertEquals(MembershipKind.WORK, membership.kind)
    }

    @Test
    fun enumsUseTheDatabaseLabels() {
        assertEquals("trabalho", MembershipKind.WORK.toDb())
        VehicleType.entries.forEach { assertEquals(it, vehicleTypeFromDb(it.toDb())) }
        assertEquals("moto", VehicleType.MOTORCYCLE.toDb())
    }
}
