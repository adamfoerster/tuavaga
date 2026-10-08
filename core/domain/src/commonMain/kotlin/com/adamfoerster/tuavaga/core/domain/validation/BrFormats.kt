package com.adamfoerster.tuavaga.core.domain.validation

/**
 * Brazilian formats typed by residents. `normalize*` accepts what people usually type
 * (lower case, hyphens, spaces, punctuation) and returns the stored form, or `null` if invalid.
 * The backend enforces the same shapes (see supabase/migrations).
 */
object BrFormats {

    private val plateRegex = Regex("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$")
    private val inviteRegex = Regex("^[A-Z]{2}[A-Z0-9]{4}$")

    /** Old (ABC1234) or Mercosul (ABC1D23) plate → "ABC1D23". */
    fun normalizePlate(input: String): String? =
        input.uppercase().filter { it.isLetterOrDigit() }.takeIf { plateRegex.matches(it) }

    /** "av4k7q", "AV-4K7Q" → "AV-4K7Q". */
    fun normalizeInviteCode(input: String): String? =
        input.uppercase().filter { it.isLetterOrDigit() }
            .takeIf { inviteRegex.matches(it) }
            ?.let { "${it.take(2)}-${it.drop(2)}" }

    /** 8 digits → "00000-000". */
    fun normalizeCep(input: String): String? =
        input.filter { it.isDigit() }.takeIf { it.length == 8 }?.let { "${it.take(5)}-${it.drop(5)}" }

    /** Landline (10 digits) or mobile (11 digits) with area code → "(11) 91234-5678". */
    fun normalizePhone(input: String): String? {
        val all = input.filter { it.isDigit() }
        // Drop the country code only when it is clearly there (55 is also an area code in RS).
        val digits = (if (all.length in 12..13 && all.startsWith("55")) all.drop(2) else all)
            .takeIf { it.length in 10..11 } ?: return null
        val area = digits.take(2)
        val number = digits.drop(2)
        val split = number.length - 4
        return "($area) ${number.take(split)}-${number.drop(split)}"
    }

    /** "A, B ,, C" → [A, B, C] (blank and repeated entries dropped). */
    fun parseList(input: String): List<String> =
        input.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
