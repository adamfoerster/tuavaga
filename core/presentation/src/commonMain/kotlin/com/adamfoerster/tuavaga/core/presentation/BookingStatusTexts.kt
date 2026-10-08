package com.adamfoerster.tuavaga.core.presentation

import com.adamfoerster.tuavaga.core.designsystem.components.KbTone
import com.adamfoerster.tuavaga.core.domain.booking.Booking
import com.adamfoerster.tuavaga.core.domain.booking.BookingActionError
import com.adamfoerster.tuavaga.core.domain.booking.BookingRole
import com.adamfoerster.tuavaga.core.domain.booking.BookingStatus
import com.adamfoerster.tuavaga.core.domain.booking.BookingVehicle
import com.adamfoerster.tuavaga.core.domain.user.shortName
import com.adamfoerster.tuavaga.core.domain.vehicle.VehicleType

/** Status tag of a booking (always a word, never only a color): text and tone. */
fun Booking.statusTag(): Pair<String, KbTone> = when (status) {
    BookingStatus.PENDING ->
        (if (role == BookingRole.OWNER) "Aguardando você" else "Aguardando aprovação") to KbTone.Caution
    BookingStatus.CONFIRMED -> "Confirmada" to KbTone.Go
    BookingStatus.IN_PROGRESS -> "Em andamento" to KbTone.Go
    BookingStatus.REJECTED -> "Recusada" to KbTone.Danger
    BookingStatus.CANCELLED -> "Cancelada" to KbTone.Danger
    BookingStatus.EXPIRED -> "Sem resposta" to KbTone.Neutral
    BookingStatus.COMPLETED -> "Concluída" to KbTone.Neutral
}

/** "Marina R." — or a neutral word when the profile has no name. */
val Booking.counterpartShortName: String
    get() = shortName(counterpart.name) ?: if (role == BookingRole.RENTER) "O locador" else "O locatário"

/** "BLOCO A · UNIDADE 31" (null parts are left out). */
val Booking.counterpartPlace: String
    get() = listOfNotNull(counterpart.block?.let { "Bloco $it" }, counterpart.unit?.let { "Unidade $it" })
        .joinToString(" · ").uppercase()

/** "Onix preto · FGH2J34 · carro" (board 17). */
fun BookingVehicle.line(): String = listOfNotNull(
    listOfNotNull(model, color?.lowercase()).joinToString(" ").ifEmpty { null },
    plate,
    when (type) {
        VehicleType.CAR -> "carro"
        VehicleType.MOTORCYCLE -> "moto"
        VehicleType.LARGE -> "veículo grande"
        null -> null
    },
).joinToString(" · ")

/** "VAGA B2-14 · SÁB 10/10 08:00 → DOM 11/10 18:00" (board 17). */
fun Booking.spotAndPeriodLine(): String =
    "Vaga $spotCode · ${period.start.date.weekdayDayMonth()} ${period.start.hhmm()} → " +
        "${period.end.date.weekdayDayMonth()} ${period.end.hhmm()}"

fun BookingActionError.toUiText(): UiText = when (this) {
    BookingActionError.InvalidState -> UiText.Dynamic("Esta reserva mudou enquanto isso. Confira o estado atual.")
    BookingActionError.Conflict -> UiText.Dynamic("Já existe reserva confirmada nesse horário. Recuse o pedido ou cancele a outra reserva.")
    BookingActionError.CancelWindowClosed -> UiText.Dynamic("O prazo para cancelar sem aviso terminou. Combine com o locador.")
    BookingActionError.CheckInClosed -> UiText.Dynamic("O check-in libera 30 min antes da entrada e vai até a saída.")
    BookingActionError.SpotUnavailable -> UiText.Dynamic("A vaga não está livre nesse horário. Escolha uma saída mais cedo.")
    BookingActionError.InvalidPeriod -> UiText.Dynamic("Escolha uma saída depois da atual.")
    BookingActionError.NotFound -> UiText.Dynamic("Esta reserva não está mais disponível.")
    is BookingActionError.Remote -> error.toUiText()
}
