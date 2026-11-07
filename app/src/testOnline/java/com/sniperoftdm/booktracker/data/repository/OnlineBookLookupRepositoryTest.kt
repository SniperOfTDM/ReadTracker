package com.sniperoftdm.booktracker.data.repository

import com.sniperoftdm.booktracker.data.remote.RequestThrottle
import com.sniperoftdm.booktracker.domain.LookupResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class OnlineBookLookupRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var testCacheDir: File

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        testCacheDir = File(System.getProperty("java.io.tmpdir"), "test_repo_cache_${System.currentTimeMillis()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        server.shutdown()
        testCacheDir.deleteRecursively()
    }

    private fun createRepository(
        contact: String = "test@example.com",
        throttleMs: Long = 0L,
        client: OkHttpClient? = null
    ): OnlineBookLookupRepository {
        val testClient = client ?: OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                val userAgent = if (contact.isNotBlank()) "BookTracker/1.0 ($contact)" else "BookTracker/1.0"
                val req = chain.request().newBuilder().header("User-Agent", userAgent).build()
                chain.proceed(req)
            }
            .build()

        val baseUrl = server.url("/").toString().removeSuffix("/")
        return OnlineBookLookupRepository(
            context = null,
            contact = contact,
            throttle = RequestThrottle(minSpacingMs = throttleMs),
            okHttpClientOverride = testClient,
            baseUrl = baseUrl,
            coversBaseUrl = baseUrl,
            cacheDirectory = testCacheDir
        )
    }

    @Test
    fun search_success_returnsMappedCandidates() = runBlocking {
        val json = """
            {
              "numFound": 1,
              "start": 0,
              "docs": [
                {
                  "key": "/works/OL1000W",
                  "title": "Clean Architecture",
                  "author_name": ["Robert C. Martin"],
                  "first_publish_year": 2017,
                  "number_of_pages_median": 432,
                  "cover_i": 12345,
                  "isbn": ["9780134494166"],
                  "edition_count": 8
                }
              ]
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))
        val repo = createRepository()

        val result = repo.search("Clean Architecture", page = 1)

        assertTrue(result is LookupResult.Success)
        val candidates = (result as LookupResult.Success).candidates
        assertEquals(1, candidates.size)
        val candidate = candidates.first()
        assertEquals("/works/OL1000W", candidate.openLibraryKey)
        assertEquals("Clean Architecture", candidate.title)
        assertEquals(listOf("Robert C. Martin"), candidate.authors)
        assertEquals(432, candidate.pages)
        assertEquals(12345L, candidate.coverId)
        assertEquals("9780134494166", candidate.isbn)
    }

    @Test
    fun search_rateLimited_returnsRateLimitedWithoutRetrying() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setBody("Rate limited"))
        val repo = createRepository()

        val result = repo.search("Kotlin Coroutines", page = 1)

        assertEquals(LookupResult.RateLimited, result)
        assertEquals("Must not retry 429 requests", 1, server.requestCount)
    }

    @Test
    fun search_serverError_returnsHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        val repo = createRepository()

        val result = repo.search("Clean Code", page = 1)

        assertTrue(result is LookupResult.HttpError)
        assertEquals(500, (result as LookupResult.HttpError).code)
    }

    @Test
    fun search_malformedJson_returnsParseError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{ bad json: broken }"))
        val repo = createRepository()

        val result = repo.search("Domain Driven Design", page = 1)

        assertEquals(LookupResult.ParseError, result)
    }

    @Test
    fun search_timeout_returnsTimeout() = runBlocking {
        server.enqueue(
            MockResponse()
                .setBody("{ }")
                .setBodyDelay(1000, TimeUnit.MILLISECONDS)
        )
        val repo = createRepository()

        val result = repo.search("Slow Server Response", page = 1)

        assertEquals(LookupResult.Timeout, result)
    }

    @Test
    fun userAgent_withContact_includesContactInHeader() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"docs\":[]}"))
        val repo = createRepository(contact = "dev@booktracker.org")

        repo.search("Test Search", page = 1)

        val recorded = server.takeRequest()
        assertEquals("BookTracker/1.0 (dev@booktracker.org)", recorded.getHeader("User-Agent"))
    }

    @Test
    fun userAgent_withoutContact_usesFallbackHeader() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"docs\":[]}"))
        val repo = createRepository(contact = "")

        repo.search("Test Search", page = 1)

        val recorded = server.takeRequest()
        assertEquals("BookTracker/1.0", recorded.getHeader("User-Agent"))
    }

    @Test
    fun lookupIsbn_invalidChecksum_returnsInvalidIsbnWithoutNetworkCall() = runBlocking {
        val repo = createRepository()

        val result = repo.lookupIsbn("978-0-14-032872-9") // Invalid checksum

        assertEquals(LookupResult.InvalidIsbn, result)
        assertEquals("No network requests should be made for invalid ISBN", 0, server.requestCount)
    }

    @Test
    fun lookupIsbn_validIsbn_makesCorrectQuery() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"docs\":[]}"))
        val repo = createRepository()

        repo.lookupIsbn("978-0-14-032872-1")

        val recorded = server.takeRequest()
        assertTrue(recorded.path!!.contains("isbn%3A9780140328721") || recorded.path!!.contains("isbn:9780140328721"))
    }

    @Test
    fun throttleSpacing_enforcesDelayBetweenCalls() = runBlocking {
        var simulatedTime = 1000L
        val fakeClock = { simulatedTime }
        val throttle = RequestThrottle(minSpacingMs = 400L, clock = fakeClock)

        val start = System.currentTimeMillis()
        throttle.acquire()
        // Simulate immediate second call
        throttle.acquire()
        val duration = System.currentTimeMillis() - start

        // Must have delayed by approximately 400ms
        assertTrue("Duration was $duration ms", duration >= 350L)
    }
}
