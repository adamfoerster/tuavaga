package com.adamfoerster.tuavaga.core.domain.user

/** "Marina Ribeiro Souza" → "Marina R." (the design never shows full surnames). */
fun shortName(fullName: String?): String? {
    val parts = fullName?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    return when {
        parts.isEmpty() -> null
        parts.size == 1 -> parts[0]
        else -> "${parts.first()} ${parts.last().first().uppercaseChar()}."
    }
}
