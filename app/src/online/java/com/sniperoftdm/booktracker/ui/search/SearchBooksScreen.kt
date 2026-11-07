package com.sniperoftdm.booktracker.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.remote.IsbnScanner
import com.sniperoftdm.booktracker.domain.BookCandidate
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayIconButton
import com.sniperoftdm.booktracker.ui.clay.ClayScreenScaffold
import com.sniperoftdm.booktracker.ui.clay.ClayTextField
import com.sniperoftdm.booktracker.ui.components.BookCoverPlaceholder
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

/**
 * Screen enabling online book searching via Open Library and ISBN barcode scanning.
 */
@Composable
fun SearchBooksScreen(
    state: SearchBooksUiState,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    onIsbnScanned: (String) -> Unit,
    onCandidateSelected: (BookCandidate) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val context = LocalContext.current

    ClayScreenScaffold(
        modifier = modifier,
        isTopLevel = false,
        navigationIcon = {
            ClayIconButton(
                onClick = onBack,
                elevation = ClayElevation.Raised,
                contentDescription = stringResource(R.string.action_cancel)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        title = {
            Text(
                text = stringResource(R.string.search_books_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(
                    start = Dimens.spaceLarge,
                    end = Dimens.spaceLarge,
                    top = Dimens.spaceSmall,
                    bottom = paddingValues.calculateBottomPadding()
                )
        ) {
            // Search Text Field with leading search icon and trailing barcode scanner
            ClayTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search_books_placeholder),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = clayColors.terracotta
                    )
                },
                trailingIcon = if (state.isScannerAvailable) {
                    {
                        IconButton(
                            onClick = {
                                IsbnScanner.startScan(
                                    context = context,
                                    onSuccess = onIsbnScanned
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = stringResource(R.string.scan_isbn_action),
                                tint = clayColors.terracotta
                            )
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(Dimens.spaceMedium))

            // Body Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        SearchLoadingPlaceholders()
                    }
                    state.error != null -> {
                        SearchErrorView(
                            error = state.error,
                            onRetry = onRetry
                        )
                    }
                    state.hasSearched && state.candidates.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_books_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium),
                            contentPadding = PaddingValues(vertical = Dimens.spaceSmall)
                        ) {
                            items(state.candidates, key = { it.openLibraryKey }) { candidate ->
                                SearchCandidateCard(
                                    candidate = candidate,
                                    onClick = { onCandidateSelected(candidate) }
                                )
                            }
                        }
                    }
                }
            }

            // Footer Disclaimer
            Text(
                text = stringResource(R.string.search_books_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spaceSmall)
            )
        }
    }
}

@Composable
private fun SearchCandidateCard(
    candidate: BookCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val coverThumbnailUrl = when {
        candidate.coverId != null -> "https://covers.openlibrary.org/b/id/${candidate.coverId}-S.jpg"
        !candidate.isbn.isNullOrBlank() -> "https://covers.openlibrary.org/b/isbn/${candidate.isbn}-S.jpg?default=false"
        else -> null
    }

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(Dimens.spaceMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverPlaceholder(
                title = candidate.title,
                author = candidate.authors.firstOrNull() ?: "",
                coverPath = coverThumbnailUrl,
                width = 48.dp,
                height = 70.dp
            )

            Spacer(modifier = Modifier.width(Dimens.spaceMedium))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = candidate.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (candidate.authors.isNotEmpty()) {
                    Text(
                        text = candidate.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (candidate.firstPublishYear != null) {
                        Text(
                            text = candidate.firstPublishYear.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = clayColors.terracotta
                        )
                    }

                    if (candidate.pages != null) {
                        Text(
                            text = pluralStringResource(R.plurals.approximate_pages_count, candidate.pages, candidate.pages),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (candidate.pages != null && candidate.pagesApproximate) {
                    Text(
                        text = stringResource(R.string.approximate_pages_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchLoadingPlaceholders(modifier: Modifier = Modifier) {
    val clayColors = LocalClayColors.current
    val transition = rememberInfiniteTransition(label = "pulseTransition")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
    ) {
        repeat(3) {
            ClayCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha),
                contentPadding = PaddingValues(Dimens.spaceMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(clayColors.edge.copy(alpha = 0.5f))
                    )

                    Spacer(modifier = Modifier.width(Dimens.spaceMedium))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(clayColors.edge.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(clayColors.edge.copy(alpha = 0.4f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(clayColors.edge.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchErrorView(
    error: SearchError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (error) {
        is SearchError.Offline -> stringResource(R.string.search_books_offline)
        is SearchError.RateLimited -> stringResource(R.string.search_books_rate_limited)
        is SearchError.InvalidIsbn -> stringResource(R.string.search_books_invalid_isbn)
        else -> stringResource(R.string.search_books_error)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.spaceLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        ClayButton(
            onClick = onRetry,
            variant = ClayButtonVariant.Tonal
        ) {
            Text(stringResource(R.string.action_retry))
        }
    }
}
