package com.adamfoerster.tuavaga.core.domain.booking

/** Mirror of the database's `booking_status`. */
enum class BookingStatus {
    /** Waiting for the owner (manual approval), until `respond_by`. */
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,

    /** Not answered in time. */
    EXPIRED,

    /** Checked in. */
    IN_PROGRESS,
    COMPLETED,
    ;

    /** Still ahead or happening (board 12 "Próximas" / "Em curso"). */
    val isActive: Boolean get() = this == PENDING || this == CONFIRMED || this == IN_PROGRESS
}
