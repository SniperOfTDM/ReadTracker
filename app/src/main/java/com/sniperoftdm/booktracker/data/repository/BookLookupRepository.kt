package com.sniperoftdm.booktracker.data.repository

import com.sniperoftdm.booktracker.domain.BookCandidate
import com.sniperoftdm.booktracker.domain.LookupResult
import java.io.File

/**
 * Repository interface for searching and fetching book metadata and covers.
 */
interface BookLookupRepository {

    /** Search for books matching query with 1-based page pagination. */
    suspend fun search(query: String, page: Int = 1): LookupResult

    /** Look up book details by 10-digit or 13-digit ISBN. */
    suspend fun lookupIsbn(isbn: String): LookupResult

    /** Download candidate cover to a temporary file, returning the file or null on failure. */
    suspend fun downloadCover(candidate: BookCandidate): File?
}
