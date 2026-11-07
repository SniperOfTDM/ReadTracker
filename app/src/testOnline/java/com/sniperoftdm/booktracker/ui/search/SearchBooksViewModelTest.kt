package com.sniperoftdm.booktracker.ui.search

import com.sniperoftdm.booktracker.data.repository.BookLookupRepository
import com.sniperoftdm.booktracker.domain.BookCandidate
import com.sniperoftdm.booktracker.domain.LookupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SearchBooksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBookLookupRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBookLookupRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onQueryChange_debouncesRequestsBy700ms() = runTest(testDispatcher) {
        val viewModel = SearchBooksViewModel(fakeRepository)

        viewModel.onQueryChange("Atomic")

        // Before 700ms elapses, no request should have been made
        advanceTimeBy(600)
        runCurrent()
        assertEquals(0, fakeRepository.searchCalls.size)
        assertFalse(viewModel.uiState.value.isLoading)

        // After 700ms threshold, request executes
        advanceTimeBy(150)
        runCurrent()
        assertEquals(1, fakeRepository.searchCalls.size)
        assertEquals("Atomic", fakeRepository.searchCalls.first())
    }

    @Test
    fun onQueryChange_cancelsSupersededSearches() = runTest(testDispatcher) {
        val viewModel = SearchBooksViewModel(fakeRepository)

        viewModel.onQueryChange("Clean")
        advanceTimeBy(400) // 400ms passes (before 700ms)
        runCurrent()

        // User types more before debounce fires
        viewModel.onQueryChange("Clean Code")
        advanceTimeBy(400)
        runCurrent()

        // Total 800ms since start, but only 400ms since "Clean Code" -> 0 calls
        assertEquals(0, fakeRepository.searchCalls.size)

        advanceTimeBy(350)
        runCurrent()

        // Now "Clean Code" fired; "Clean" was cancelled and never called
        assertEquals(1, fakeRepository.searchCalls.size)
        assertEquals("Clean Code", fakeRepository.searchCalls.first())
    }

    @Test
    fun onQueryChange_shortQuery_doesNotTriggerSearch() = runTest(testDispatcher) {
        val viewModel = SearchBooksViewModel(fakeRepository)

        viewModel.onQueryChange("ab")
        advanceTimeBy(1000)
        runCurrent()

        assertEquals(0, fakeRepository.searchCalls.size)
        assertFalse(viewModel.uiState.value.hasSearched)
    }

    private class FakeBookLookupRepository : BookLookupRepository {
        val searchCalls = mutableListOf<String>()

        override suspend fun search(query: String, page: Int): LookupResult {
            searchCalls.add(query)
            return LookupResult.Success(
                listOf(
                    BookCandidate(
                        openLibraryKey = "/works/1",
                        title = query,
                        authors = listOf("Author"),
                        pages = 250
                    )
                )
            )
        }

        override suspend fun lookupIsbn(isbn: String): LookupResult {
            return LookupResult.Success(emptyList())
        }

        override suspend fun downloadCover(candidate: BookCandidate): File? = null
    }
}
