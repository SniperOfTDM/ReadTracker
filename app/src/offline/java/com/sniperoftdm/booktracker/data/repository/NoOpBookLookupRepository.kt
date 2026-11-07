package com.sniperoftdm.booktracker.data.repository

import com.sniperoftdm.booktracker.domain.BookCandidate
import com.sniperoftdm.booktracker.domain.LookupResult
import java.io.File

/**
 * Offline implementation of BookLookupRepository that always reports lookups as unavailable.
 */
class NoOpBookLookupRepository : BookLookupRepository {

    /** Always returns Unavailable in offline flavor. */
    override suspend fun search(query: String, page: Int): LookupResult = LookupResult.Unavailable

    /** Always returns Unavailable in offline flavor. */
    override suspend fun lookupIsbn(isbn: String): LookupResult = LookupResult.Unavailable

    /** Always returns null in offline flavor without downloading covers. */
    override suspend fun downloadCover(candidate: BookCandidate): File? = null
}
