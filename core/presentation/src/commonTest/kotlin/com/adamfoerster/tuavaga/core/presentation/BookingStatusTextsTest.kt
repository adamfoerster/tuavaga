package com.adamfoerster.tuavaga.core.presentation

import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingPeriod
import com.adamfoerster.tuavaga.core.domain.booking.BookingQuote
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.BookingVehicle
import com.adamfoerster.tuavaga.core.domain.booking.Counterpart
import com.adamfoerster.tuavaga.core.domain.spot.BillingUnit
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BookingStatusTextsTest {

    private val request = Booking(
        id = "b1", code = 4821, role = BookingRole.OWNER, status = BookingStatus.PENDING, condoId = "c1",
        condoName = "Residencial Alameda Verde", spotId = "s1", levelName = "Subsolo 2", sectorName = "B", spotNumber = "14",
        directions = null, rules = emptyList(), cancelNoticeHours = 24,
        period = BookingPeriod(LocalDateTime(2026, 10, 10, 8, 0), LocalDateTime(2026, 10, 11, 18, 0)),
        quote = BookingQuote(BillingUnit.DAY, 2, 3500), note = null, rejectReason = null, rejectMessage = null,
        respondBy = null, cancelledByOwner = null, checkedInAt = null, checkedOutAt = null,
        counterpart = Counterpart("Rafael Souza Lima", "C", null),
        vehicle = BookingVehicle("FGH2J34", "Onix", "Preto", VehicleType.CAR), conflict = null,
    )

    @Test
    fun statusIsAlwaysAWord() {
        assertEquals("Aguardando você" to KbTone.Caution, request.statusTag())
        assertEquals("Aguardando aprovação" to KbTone.Caution, request.copy(role = BookingRole.RENTER).statusTag())
        assertEquals("Em andamento" to KbTone.Go, request.copy(status = BookingStatus.IN_PROGRESS).statusTag())
        assertEquals("Recusada" to KbTone.Danger, request.copy(status = BookingStatus.REJECTED).statusTag())
    }

    @Test
    fun requestCardLines() {
        // Board 17.
        assertEquals("Onix preto · FGH2J34 · carro", request.vehicle?.line())
        assertEquals("Vaga B2-14 · Sáb 10/10 08:00 → Dom 11/10 18:00", request.spotAndPeriodLine())
        assertEquals("Rafael L.", request.counterpartShortName)
        assertEquals("BLOCO C", request.counterpartPlace)
        assertEquals("O locatário", request.copy(counterpart = Counterpart(null, null, null)).counterpartShortName)
    }
}
