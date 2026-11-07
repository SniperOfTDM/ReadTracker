package com.sniperoftdm.booktracker.flavor

import android.content.Context
import com.sniperoftdm.booktracker.data.repository.BookLookupRepository
import com.sniperoftdm.booktracker.data.repository.OnlineBookLookupRepository

/**
 * Provides flavor-specific dependencies for the online variant.
 */
object FlavorDependencies {

    private var repositoryInstance: OnlineBookLookupRepository? = null

    /** Provide the online implementation of BookLookupRepository. */
    fun provideBookLookupRepository(context: Context): BookLookupRepository {
        return repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: OnlineBookLookupRepository(context.applicationContext).also {
                repositoryInstance = it
            }
        }
    }
}
