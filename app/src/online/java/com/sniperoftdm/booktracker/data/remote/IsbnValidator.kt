package com.sniperoftdm.booktracker.data.remote

/**
 * Validates ISBN-10 and ISBN-13 numbers with checksum algorithm verification.
 */
object IsbnValidator {

    /** Clean formatting characters from ISBN. */
    fun clean(isbn: String): String {
        return isbn.replace("-", "").replace(" ", "").trim()
    }

    /** Returns true if the raw input is a valid ISBN-10 with correct checksum. */
    fun isValidIsbn10(isbn: String): Boolean {
        val clean = clean(isbn)
        if (clean.length != 10) return false

        var sum = 0
        for (i in 0 until 9) {
            val char = clean[i]
            if (!char.isDigit()) return false
            sum += (char - '0') * (10 - i)
        }

        val checkChar = clean[9]
        val checkValue = when {
            checkChar == 'X' || checkChar == 'x' -> 10
            checkChar.isDigit() -> checkChar - '0'
            else -> return false
        }
        sum += checkValue

        return sum % 11 == 0
    }

    /** Returns true if the raw input is a valid ISBN-13 with correct checksum. */
    fun isValidIsbn13(isbn: String): Boolean {
        val clean = clean(isbn)
        if (clean.length != 13) return false

        var sum = 0
        for (i in 0 until 13) {
            val char = clean[i]
            if (!char.isDigit()) return false
            val weight = if (i % 2 == 0) 1 else 3
            sum += (char - '0') * weight
        }

        return sum % 10 == 0
    }

    /** Returns true if the string is a valid ISBN-10 or ISBN-13. */
    fun isValid(isbn: String): Boolean {
        val clean = clean(isbn)
        return when (clean.length) {
            10 -> isValidIsbn10(clean)
            13 -> isValidIsbn13(clean)
            else -> false
        }
    }
}
