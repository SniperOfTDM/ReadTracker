package com.sniperoftdm.booktracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open Library search response data structure.
 */
@Serializable
data class OpenLibrarySearchResponse(
    val numFound: Int = 0,
    val start: Int = 0,
    val docs: List<OpenLibraryDoc> = emptyList()
)

/**
 * Open Library document representing a candidate book record.
 */
@Serializable
data class OpenLibraryDoc(
    val key: String,
    val title: String = "",
    @SerialName("author_name") val authorName: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("number_of_pages_median") val numberOfPagesMedian: Int? = null,
    @SerialName("cover_i") val coverI: Long? = null,
    val isbn: List<String> = emptyList(),
    @SerialName("edition_count") val editionCount: Int? = null
)
