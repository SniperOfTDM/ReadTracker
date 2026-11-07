package com.sniperoftdm.booktracker.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenLibraryParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun loadFixture(filename: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("openlibrary/$filename")
            ?: throw IllegalArgumentException("Fixture openlibrary/$filename not found")
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun parse_missingCover_returnsCandidateWithNullCover() {
        val content = loadFixture("missing_cover.json")
        val response = json.decodeFromString<OpenLibrarySearchResponse>(content)

        assertEquals(1, response.docs.size)
        val doc = response.docs.first()
        assertEquals("/works/OL12345W", doc.key)
        assertNull(doc.coverI)
        assertEquals(350, doc.numberOfPagesMedian)
    }

    @Test
    fun parse_missingPages_returnsCandidateWithNullPages() {
        val content = loadFixture("missing_pages.json")
        val response = json.decodeFromString<OpenLibrarySearchResponse>(content)

        assertEquals(1, response.docs.size)
        val doc = response.docs.first()
        assertEquals("/works/OL54321W", doc.key)
        assertNull(doc.numberOfPagesMedian)
        assertEquals(1234567L, doc.coverI)
    }

    @Test
    fun parse_multipleAuthors_preservesAllAuthors() {
        val content = loadFixture("multiple_authors.json")
        val response = json.decodeFromString<OpenLibrarySearchResponse>(content)

        assertEquals(1, response.docs.size)
        val doc = response.docs.first()
        assertEquals(listOf("Alice Smith", "Bob Jones", "Carol White"), doc.authorName)
    }

    @Test
    fun parse_unknownExtraFields_ignoresSilentlyWithoutFailing() {
        val content = loadFixture("unknown_extra_field.json")
        val response = json.decodeFromString<OpenLibrarySearchResponse>(content)

        assertEquals(1, response.docs.size)
        val doc = response.docs.first()
        assertEquals("Future Proof Book", doc.title)
    }

    @Test
    fun parse_emptyDocs_returnsEmptyList() {
        val content = loadFixture("empty_docs.json")
        val response = json.decodeFromString<OpenLibrarySearchResponse>(content)

        assertTrue(response.docs.isEmpty())
        assertEquals(0, response.numFound)
    }
}
