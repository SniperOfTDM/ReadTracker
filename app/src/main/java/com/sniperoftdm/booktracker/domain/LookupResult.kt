package com.sniperoftdm.booktracker.domain

/**
 * Sealed outcome for book lookups and searches without throwing exceptions to ViewModels.
 */
sealed interface LookupResult {
    data class Success(val candidates: List<BookCandidate>) : LookupResult
    data object Unavailable : LookupResult
    data object Offline : LookupResult
    data object RateLimited : LookupResult
    data object Timeout : LookupResult
    data class HttpError(val code: Int) : LookupResult
    data object ParseError : LookupResult
    data object InvalidIsbn : LookupResult
}
