package com.adamfoerster.tuavaga.feature.bookings.presentation.common

/** "2 d 4 h", "3 h 20 min", "42 min" (countdowns and delays). */
fun durationShort(minutes: Int): String {
    val days = minutes / (24 * 60)
    val hours = minutes % (24 * 60) / 60
    val mins = minutes % 60
    return when {
        days > 0 -> if (hours > 0) "$days d $hours h" else "$days d"
        hours > 0 -> if (mins > 0) "$hours h $mins min" else "$hours h"
        else -> "$mins min"
    }
}
