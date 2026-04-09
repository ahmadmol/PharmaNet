package com.pharmalink.core.common.validation

/**
 * Syrian mobile numbers: ITU country code 963, followed by a 9-digit national mobile
 * number starting with 9 (e.g. +963 9XX XXX XXX).
 *
 * Accepted input shapes (non-exhaustive): +963 9XX XXX XXX, 9639XXXXXXXX, 09XXXXXXXX, 9XXXXXXXX
 */
object SyrianPhone {

    fun digitsOnly(input: String): String = input.filter { it.isDigit() }

    /**
     * Returns normalized digits without "+" in E.164 form, e.g. "963912345678", or null if invalid.
     */
    fun normalizeToE164Digits(input: String): String? {
        var d = digitsOnly(input)
        if (d.startsWith("00963")) {
            d = d.removePrefix("00")
        }
        return when {
            d.startsWith("963") && d.length >= 12 -> {
                val national = d.drop(3).take(9)
                if (national.length == 9 && national[0] == '9') {
                    "963$national"
                } else {
                    null
                }
            }
            d.length == 9 && d[0] == '9' -> "963$d"
            d.length == 10 && d[0] == '0' && d[1] == '9' -> "963${d.drop(1)}"
            else -> null
        }
    }

    fun isValid(input: String): Boolean = normalizeToE164Digits(input) != null
}
