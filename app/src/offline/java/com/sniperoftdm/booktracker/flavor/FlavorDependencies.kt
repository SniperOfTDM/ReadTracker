package com.sniperoftdm.booktracker.flavor

import android.content.Context
import com.sniperoftdm.booktracker.data.repository.BookLookupRepository
import com.sniperoftdm.booktracker.data.repository.NoOpBookLookupRepository

/**
 * Provides flavor-specific dependencies for the offline variant.
 */
object FlavorDependencies {

    /** Provide the offline no-op implementation of BookLookupRepository. */
    fun provideBookLookupRepository(context: Context): BookLookupRepository = NoOpBookLookupRepository()
}
