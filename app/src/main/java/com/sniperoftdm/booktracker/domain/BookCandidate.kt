package com.sniperoftdm.booktracker.domain

import kotlinx.serialization.Serializable

/**
 * Representation of a book search candidate from Open Library or manual lookup.
 */
@Serializable
data class BookCandidate(
    val openLibraryKey: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val firstPublishYear: Int? = null,
    val pages: Int? = null,
    val coverId: Long? = null,
    val isbn: String? = null,
    val editionCount: Int? = null,
    val pagesApproximate: Boolean = true
)
