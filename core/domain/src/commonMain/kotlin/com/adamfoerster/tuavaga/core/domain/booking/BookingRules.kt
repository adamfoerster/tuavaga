package com.adamfoerster.tuavaga.core.domain.booking

import com.adamfoerster.tuavaga.core.domain.time.APP_TIME_ZONE
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import kotlinx.datetime.toInstant

// Mirror of the checks in the booking_lifecycle migration, so the screens only offer what the
// server accepts. `now` is Brasília local time (appNow()).

/** How early the check-in opens (check_in in the migration). */
const val CHECK_IN_EARLY_MINUTES = 30

/** Tabs of board 12. */
enum class BookingTab { UPCOMING, ONGOING, HISTORY }

val Booking.tab: BookingTab
    get() = when (status) {
        BookingStatus.PENDING, BookingStatus.CONFIRMED -> BookingTab.UPCOMING
        BookingStatus.IN_PROGRESS -> BookingTab.ONGOING
        else -> BookingTab.HISTORY
    }

/** Last moment the renter cancels a confirmed booking alone ("Cancelamento sem aviso até …"). */
val Booking.cancelDeadline: LocalDateTime get() = period.start.plusMinutes(-cancelNoticeHours * 60)

val Booking.checkInOpensAt: LocalDateTime get() = period.start.plusMinutes(-CHECK_IN_EARLY_MINUTES)

fun Booking.canCancel(now: LocalDateTime): Boolean = when (role) {
    BookingRole.RENTER -> status == BookingStatus.PENDING || (status == BookingStatus.CONFIRMED && now <= cancelDeadline)
    BookingRole.OWNER -> status == BookingStatus.CONFIRMED && now < period.start
}

/** Confirmed, but the cancellation period is over: the renter has to talk to the owner. */
fun Booking.isPastCancelDeadline(now: LocalDateTime): Boolean =
    role == BookingRole.RENTER && status == BookingStatus.CONFIRMED && now > cancelDeadline

fun Booking.canCheckIn(now: LocalDateTime): Boolean =
    role == BookingRole.RENTER && status == BookingStatus.CONFIRMED && now >= checkInOpensAt && now < period.end

val Booking.canCheckOut: Boolean get() = role == BookingRole.RENTER && status == BookingStatus.IN_PROGRESS

val Booking.canExtend: Boolean
    get() = role == BookingRole.RENTER && (status == BookingStatus.CONFIRMED || status == BookingStatus.IN_PROGRESS)

val Booking.canAnswer: Boolean get() = role == BookingRole.OWNER && status == BookingStatus.PENDING

/** Minutes past the agreed exit while still parked ("Passou do horário"); 0 when not late. */
fun Booking.minutesLate(now: LocalDateTime): Int =
    if (status == BookingStatus.IN_PROGRESS && now > period.end) minutesBetween(period.end, now) else 0

/** Whole minutes from [from] to [to] (negative when [to] is earlier). */
fun minutesBetween(from: LocalDateTime, to: LocalDateTime): Int =
    (to.toInstant(APP_TIME_ZONE) - from.toInstant(APP_TIME_ZONE)).inWholeMinutes.toInt()

/** Bookings that pay the owner: confirmed, in progress or completed. */
private val EARNING = setOf(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)

/** Owner bookings that count for [year]/[month] (by start), optionally of one spot ("Ganhos do mês"). */
fun List<Booking>.ownerBookingsIn(year: Int, month: Int, spotId: String? = null): List<Booking> = filter {
    it.role == BookingRole.OWNER && it.status in EARNING &&
        it.period.start.year == year && it.period.start.month.number == month &&
        (spotId == null || it.spotId == spotId)
}

/** Requests waiting for the owner, oldest deadline first (board 17). */
fun List<Booking>.pendingRequests(): List<Booking> =
    filter { it.canAnswer }.sortedBy { it.respondBy ?: it.period.start }
