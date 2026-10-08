package com.adamfoerster.tuavaga.feature.hosting.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** A time range inside one day, in minutes from midnight (end exclusive, up to 24:00). */
data class TimeWindow(val startMinutes: Int, val endMinutes: Int) {
    init {
        require(startMinutes in 0 until MINUTES_PER_DAY && endMinutes in 1..MINUTES_PER_DAY && startMinutes < endMinutes) {
            "Invalid window $startMinutes..$endMinutes"
        }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

/** Exception to the weekly rule on one date. */
sealed interface DayOverride {
    /** Available this day (outside the weekly rule, or with another time). */
    data class Open(val window: TimeWindow) : DayOverride

    /** Not available this day even if the weekly rule says so. */
    data object Blocked : DayOverride
}

/** What a date looks like for someone booking. */
sealed interface DayAvailability {
    data class Open(val window: TimeWindow) : DayAvailability
    data object Blocked : DayAvailability
    data object Closed : DayAvailability
}

/**
 * When a spot can be rented: a weekly rule (same window on each chosen weekday, board 15
 * "Repetir") plus per-date exceptions (board 15 "Liberar" / "Bloquear").
 */
data class Availability(
    val weekly: Map<DayOfWeek, TimeWindow> = emptyMap(),
    val overrides: Map<LocalDate, DayOverride> = emptyMap(),
) {
    /** No weekly window and no opened date: nobody could ever book it. */
    val isEmpty: Boolean get() = weekly.isEmpty() && overrides.values.none { it is DayOverride.Open }

    fun on(date: LocalDate): DayAvailability = when (val override = overrides[date]) {
        DayOverride.Blocked -> DayAvailability.Blocked
        is DayOverride.Open -> DayAvailability.Open(override.window)
        null -> weekly[date.dayOfWeek]?.let { DayAvailability.Open(it) } ?: DayAvailability.Closed
    }

    /** "Liberar": the dates become available with [window] (no exception kept when the weekly rule already matches). */
    fun open(dates: Collection<LocalDate>, window: TimeWindow): Availability = copy(
        overrides = overrides.toMutableMap().apply {
            dates.forEach { date ->
                if (weekly[date.dayOfWeek] == window) remove(date) else put(date, DayOverride.Open(window))
            }
        },
    )

    /** "Bloquear": the dates become unavailable. */
    fun block(dates: Collection<LocalDate>): Availability =
        copy(overrides = overrides + dates.associateWith { DayOverride.Blocked })

    /**
     * "Aplicar ao calendário": replaces the weekly rule. Opened dates that the new rule already
     * covers with the same window stop being exceptions; blocked dates stay blocked.
     */
    fun withWeekly(days: Set<DayOfWeek>, window: TimeWindow): Availability {
        val newWeekly = days.associateWith { window }
        return Availability(
            weekly = newWeekly,
            overrides = overrides.filter { (date, override) ->
                !(override is DayOverride.Open && newWeekly[date.dayOfWeek] == override.window)
            },
        )
    }

    /** Drops exceptions before [today]; they no longer matter and only grow the saved data. */
    fun withoutPastOverrides(today: LocalDate): Availability = copy(overrides = overrides.filterKeys { it >= today })
}

/** "Frequência" options of the repeat panel. */
enum class RepeatFrequency(val days: Set<DayOfWeek>) {
    WEEKDAYS(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)),
    WEEKENDS(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
    EVERY_DAY(DayOfWeek.entries.toSet()),

    /** No weekly rule: only the dates opened in the calendar. */
    ONLY_SELECTED(emptySet()),
}
