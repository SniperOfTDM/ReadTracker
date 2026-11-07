package com.sniperoftdm.booktracker.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsbnValidatorTest {

    @Test
    fun clean_stripsHyphensAndSpaces() {
        assertEquals("9780140328721", IsbnValidator.clean("978-0-14-032872-1"))
        assertEquals("0471958697", IsbnValidator.clean("0 471 95869 7"))
    }

    @Test
    fun isValid_validIsbn10_returnsTrue() {
        // Valid ISBN-10 with numeric check digit
        assertTrue(IsbnValidator.isValid("0-471-95869-7"))
        assertTrue(IsbnValidator.isValid("0471958697"))

        // Valid ISBN-10 ending with 'X' checksum (10)
        assertTrue(IsbnValidator.isValid("0-8044-2957-X"))
        assertTrue(IsbnValidator.isValid("080442957x"))
    }

    @Test
    fun isValid_validIsbn13_returnsTrue() {
        assertTrue(IsbnValidator.isValid("978-0-14-032872-1"))
        assertTrue(IsbnValidator.isValid("9780140328721"))
        assertTrue(IsbnValidator.isValid("978-3-16-148410-0"))
    }

    @Test
    fun isValid_badChecksum_returnsFalse() {
        // Invalid check digit for ISBN-10
        assertFalse(IsbnValidator.isValid("0-471-95869-8"))

        // Invalid check digit for ISBN-13
        assertFalse(IsbnValidator.isValid("978-0-14-032872-9"))
    }

    @Test
    fun isValid_lettersOrInvalidLength_returnsFalse() {
        assertFalse(IsbnValidator.isValid("ABCDEFGHIJ"))
        assertFalse(IsbnValidator.isValid("12345"))
        assertFalse(IsbnValidator.isValid(""))
        assertFalse(IsbnValidator.isValid("978-0-14-032872-100"))
    }
}
