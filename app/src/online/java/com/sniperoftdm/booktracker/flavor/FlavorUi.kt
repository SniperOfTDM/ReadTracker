package com.sniperoftdm.booktracker.flavor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sniperoftdm.booktracker.AppContainer
import com.sniperoftdm.booktracker.BookTrackerApp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.remote.IsbnScanner
import com.sniperoftdm.booktracker.domain.BookCandidate
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayIconButton
import com.sniperoftdm.booktracker.ui.navigation.SearchOnlineRoute
import com.sniperoftdm.booktracker.ui.search.SearchBooksScreen
import com.sniperoftdm.booktracker.ui.search.SearchBooksViewModel
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * UI entry points and hooks specialized for the online flavor.
 */
object FlavorUi {

    /** Button launching online book search on Add Book screen. */
    @Composable
    fun SearchOnlineButton(
        onNavigateToSearch: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        ClayButton(
            onClick = onNavigateToSearch,
            variant = ClayButtonVariant.Tonal,
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.search_online_action))
        }
    }

    /** Button launching ISBN barcode scanner if Google Play services are available. */
    @Composable
    fun ScanIsbnButton(
        onScanResult: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val isAvailable = remember { IsbnScanner.isAvailable(context) }
        if (isAvailable) {
            ClayIconButton(
                onClick = {
                    IsbnScanner.startScan(
                        context = context,
                        onSuccess = onScanResult
                    )
                },
                contentDescription = stringResource(R.string.scan_isbn_action),
                modifier = modifier
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = LocalClayColors.current.terracotta
                )
            }
        }
    }

    /** Button in Settings to re-download missing covers for online-linked books. */
    @Composable
    fun RedownloadCoversButton(
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val container = remember(context) { (context.applicationContext as BookTrackerApp).container }
        val scope = rememberCoroutineScope()

        var isDownloading by remember { mutableStateOf(false) }
        var currentProgress by remember { mutableIntStateOf(0) }
        var totalToDownload by remember { mutableIntStateOf(0) }
        var downloadJob by remember { mutableStateOf<Job?>(null) }
        var statusMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!isDownloading) {
                ClayButton(
                    onClick = {
                        downloadJob = scope.launch {
                            isDownloading = true
                            statusMessage = null
                            val allBooks = container.repository.observeBooks().first()
                            val missingCoverBooks = allBooks.filter {
                                it.coverPath.isNullOrBlank() && (!it.openLibraryKey.isNullOrBlank() || !it.isbn.isNullOrBlank())
                            }
                            if (missingCoverBooks.isEmpty()) {
                                statusMessage = context.getString(R.string.redownload_covers_all_present)
                                isDownloading = false
                                return@launch
                            }

                            totalToDownload = missingCoverBooks.size
                            currentProgress = 0

                            for ((index, book) in missingCoverBooks.withIndex()) {
                                if (!isActive) break
                                currentProgress = index + 1
                                val candidate = BookCandidate(
                                    openLibraryKey = book.openLibraryKey ?: "",
                                    title = book.title,
                                    authors = if (book.author.isNotBlank()) listOf(book.author) else emptyList(),
                                    isbn = book.isbn
                                )
                                try {
                                    val tempFile = container.bookLookupRepository.downloadCover(candidate)
                                    if (tempFile != null) {
                                        val saved = container.coverStorage.saveFromFile(tempFile, book.id)
                                        tempFile.delete()
                                        if (saved != null) {
                                            container.repository.updateBook(book.copy(coverPath = saved))
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            isDownloading = false
                        }
                    },
                    variant = ClayButtonVariant.Tonal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(stringResource(R.string.redownload_covers_action))
                }
                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.redownload_covers_progress, currentProgress, totalToDownload),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ClayButton(
                        onClick = {
                            downloadJob?.cancel()
                            isDownloading = false
                        },
                        variant = ClayButtonVariant.Tonal
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }

    /** Register online search route in navigation graph. */
    fun NavGraphBuilder.flavorRoutes(
        navController: NavHostController,
        appContainer: AppContainer
    ) {
        composable<SearchOnlineRoute> {
            val context = LocalContext.current
            val isScannerAvailable = remember { IsbnScanner.isAvailable(context) }
            val viewModel = remember {
                SearchBooksViewModel(
                    repository = appContainer.bookLookupRepository,
                    isScannerAvailable = isScannerAvailable
                )
            }
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            SearchBooksScreen(
                state = state,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearchAction = { viewModel.onSearchAction() },
                onIsbnScanned = { viewModel.onIsbnScanned(it) },
                onCandidateSelected = { candidate ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_candidate",
                        Json.encodeToString(candidate)
                    )
                    navController.popBackStack()
                },
                onRetry = { viewModel.retry() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
