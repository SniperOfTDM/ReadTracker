package com.sniperoftdm.booktracker.data.repository

import android.content.Context
import com.sniperoftdm.booktracker.BuildConfig
import com.sniperoftdm.booktracker.data.remote.IsbnValidator
import com.sniperoftdm.booktracker.data.remote.OpenLibrarySearchResponse
import com.sniperoftdm.booktracker.data.remote.RequestThrottle
import com.sniperoftdm.booktracker.domain.BookCandidate
import com.sniperoftdm.booktracker.domain.LookupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Production implementation of BookLookupRepository contacting Open Library via OkHttp.
 */
class OnlineBookLookupRepository(
    context: Context? = null,
    private val contact: String = BuildConfig.OPENLIBRARY_CONTACT,
    val throttle: RequestThrottle = RequestThrottle(
        minSpacingMs = if (contact.isNotBlank()) 400L else 1100L
    ),
    okHttpClientOverride: OkHttpClient? = null,
    private val baseUrl: String = "https://openlibrary.org",
    private val coversBaseUrl: String = "https://covers.openlibrary.org",
    private val cacheDirectory: File = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
) : BookLookupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val memoryCache = object : LinkedHashMap<String, LookupResult.Success>(20, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LookupResult.Success>?): Boolean {
            return size > 20
        }
    }

    val client: OkHttpClient = okHttpClientOverride ?: createDefaultOkHttpClient(cacheDirectory, contact)

    override suspend fun search(query: String, page: Int): LookupResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim().take(100)
        if (trimmed.length < 3) {
            return@withContext LookupResult.Success(emptyList())
        }

        val cacheKey = "$trimmed:$page"
        synchronized(memoryCache) {
            memoryCache[cacheKey]?.let { return@withContext it }
        }

        throttle.acquire()

        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val url = "$baseUrl/search.json?q=$encoded&limit=20&page=$page&fields=key,title,author_name,first_publish_year,number_of_pages_median,cover_i,isbn,edition_count"

        executeSearchRequest(url, cacheKey)
    }

    override suspend fun lookupIsbn(isbn: String): LookupResult = withContext(Dispatchers.IO) {
        val clean = IsbnValidator.clean(isbn)
        if (!IsbnValidator.isValid(clean)) {
            return@withContext LookupResult.InvalidIsbn
        }

        val cacheKey = "isbn:$clean"
        synchronized(memoryCache) {
            memoryCache[cacheKey]?.let { return@withContext it }
        }

        throttle.acquire()

        val encoded = URLEncoder.encode(clean, "UTF-8")
        val url = "$baseUrl/search.json?q=isbn:$encoded&limit=20&fields=key,title,author_name,first_publish_year,number_of_pages_median,cover_i,isbn,edition_count"

        executeSearchRequest(url, cacheKey)
    }

    private fun executeSearchRequest(url: String, cacheKey: String?): LookupResult {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val bodyString = response.body?.string() ?: ""
                        try {
                            val parsed = json.decodeFromString<OpenLibrarySearchResponse>(bodyString)
                            val candidates = parsed.docs.map { doc ->
                                BookCandidate(
                                    openLibraryKey = doc.key,
                                    title = doc.title,
                                    authors = doc.authorName,
                                    firstPublishYear = doc.firstPublishYear,
                                    pages = doc.numberOfPagesMedian,
                                    coverId = doc.coverI,
                                    isbn = doc.isbn.firstOrNull(),
                                    editionCount = doc.editionCount,
                                    pagesApproximate = true
                                )
                            }
                            val success = LookupResult.Success(candidates)
                            if (cacheKey != null) {
                                synchronized(memoryCache) {
                                    memoryCache[cacheKey] = success
                                }
                            }
                            success
                        } catch (e: Exception) {
                            LookupResult.ParseError
                        }
                    }
                    429 -> LookupResult.RateLimited
                    else -> LookupResult.HttpError(response.code)
                }
            }
        } catch (e: SocketTimeoutException) {
            LookupResult.Timeout
        } catch (e: UnknownHostException) {
            LookupResult.Offline
        } catch (e: IOException) {
            LookupResult.Offline
        } catch (e: Exception) {
            LookupResult.ParseError
        }
    }

    override suspend fun downloadCover(candidate: BookCandidate): File? = withContext(Dispatchers.IO) {
        val coverUrl = when {
            candidate.coverId != null -> "$coversBaseUrl/b/id/${candidate.coverId}-M.jpg"
            !candidate.isbn.isNullOrBlank() -> "$coversBaseUrl/b/isbn/${candidate.isbn}-M.jpg?default=false"
            else -> return@withContext null
        }

        throttle.acquire()

        val request = Request.Builder()
            .url(coverUrl)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val tempFile = File.createTempFile("ol_cover_", ".jpg", cacheDirectory)
                FileOutputStream(tempFile).use { out ->
                    body.byteStream().copyTo(out)
                }
                tempFile
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Create default production OkHttpClient configured with cache and headers. */
        fun createDefaultOkHttpClient(cacheDirectory: File, contact: String): OkHttpClient {
            val userAgent = if (contact.isNotBlank()) {
                "BookTracker/1.0 ($contact)"
            } else {
                "BookTracker/1.0"
            }

            val cacheDir = File(cacheDirectory, "http_cache").apply { mkdirs() }
            val cache = Cache(cacheDir, 10L * 1024L * 1024L)

            return OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .cache(cache)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestWithUa = original.newBuilder()
                        .header("User-Agent", userAgent)
                        .build()

                    var response: Response
                    try {
                        response = chain.proceed(requestWithUa)
                    } catch (e: SocketException) {
                        // Connection reset: 1 retry only
                        response = chain.proceed(requestWithUa)
                    }
                    response
                }
                .addNetworkInterceptor { chain ->
                    val request = chain.request()
                    if (!request.url.isHttps) {
                        throw IOException("Non-HTTPS request blocked: ${request.url}")
                    }
                    chain.proceed(request)
                }
                .followRedirects(true)
                .followSslRedirects(false)
                .build()
        }
    }
}
