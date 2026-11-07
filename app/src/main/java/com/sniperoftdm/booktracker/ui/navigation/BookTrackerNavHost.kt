package com.sniperoftdm.booktracker.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.ui.platform.LocalContext
import com.sniperoftdm.booktracker.flavor.FlavorUi
import com.sniperoftdm.booktracker.AppContainer
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.BookStatus
import com.sniperoftdm.booktracker.data.repository.ThemeMode
import com.sniperoftdm.booktracker.ui.book.AddEditBookEvent
import com.sniperoftdm.booktracker.ui.book.AddEditBookScreen
import com.sniperoftdm.booktracker.ui.book.AddEditBookViewModel
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayDialog
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayGalleryScreen
import com.sniperoftdm.booktracker.ui.clay.ClayNavBar
import com.sniperoftdm.booktracker.ui.clay.ClayNavItem
import com.sniperoftdm.booktracker.ui.clay.ClaySnackbar
import com.sniperoftdm.booktracker.ui.debug.DebugScreen
import com.sniperoftdm.booktracker.ui.home.HomeScreen
import com.sniperoftdm.booktracker.ui.home.HomeViewModel
import com.sniperoftdm.booktracker.ui.library.LibraryScreen
import com.sniperoftdm.booktracker.ui.library.LibraryViewModel
import com.sniperoftdm.booktracker.ui.log.QuickLogBottomSheet
import com.sniperoftdm.booktracker.ui.log.QuickLogViewModel
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTheme
import com.sniperoftdm.booktracker.ui.theme.Dimens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BookTrackerNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val settingsState by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.sniperoftdm.booktracker.data.repository.UserSettings()
    )

    BookTrackerTheme(
        themeMode = settingsState.themeMode,
        useDynamicColor = settingsState.useDynamicColor
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val scope = rememberCoroutineScope()
        var quickLogBookId by remember { mutableStateOf<Long?>(null) }
        var isQuickLogOpen by remember { mutableStateOf(false) }

        // Undo session state
        var lastSavedInfo by remember { mutableStateOf<Pair<Long, Long>?>(null) } // sessionId to bookId
        var snackbarMessage by remember { mutableStateOf<String?>(null) }

        // Book Finished Dialog state
        var finishedBookDialogData by remember { mutableStateOf<Book?>(null) }

        val navItems = remember {
            listOf(
                ClayNavItem("Home", Icons.Rounded.Home, Icons.Rounded.Home, HomeRoute),
                ClayNavItem("Library", Icons.AutoMirrored.Rounded.MenuBook, Icons.AutoMirrored.Rounded.MenuBook, LibraryRoute),
                ClayNavItem("Stats", Icons.Rounded.BarChart, Icons.Rounded.BarChart, StatsRoute),
                ClayNavItem("Settings", Icons.Rounded.Settings, Icons.Rounded.Settings, SettingsRoute)
            )
        }

        val currentRoute = navBackStackEntry?.destination?.route

        val isTopLevelRoute = currentRoute?.let { route ->
            route.endsWith("HomeRoute") ||
                    route.endsWith("LibraryRoute") ||
                    route.endsWith("StatsRoute") ||
                    route.endsWith("SettingsRoute")
        } ?: true

        val currentNavItem = when {
            currentRoute?.endsWith("LibraryRoute") == true -> LibraryRoute
            currentRoute?.endsWith("StatsRoute") == true -> StatsRoute
            currentRoute?.endsWith("SettingsRoute") == true -> SettingsRoute
            else -> HomeRoute
        }

        Box(modifier = modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                // Home Route
                composable<HomeRoute> {
                    val homeViewModel = remember {
                        HomeViewModel(
                            repository = container.repository,
                            timerRepository = container.timerRepository,
                            settingsRepository = container.settingsRepository,
                            statsCalculator = container.statsCalculator
                        )
                    }
                    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()

                    HomeScreen(
                        state = homeState,
                        onNavigateToDetail = { bookId ->
                            navController.navigate(BookDetailRoute(bookId))
                        },
                        onNavigateToAddBook = {
                            navController.navigate(AddBookRoute)
                        },
                        onNavigateToLibrary = {
                            navController.navigate(LibraryRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToStats = {
                            navController.navigate(StatsRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenQuickLog = { preselectedBookId ->
                            quickLogBookId = preselectedBookId
                            isQuickLogOpen = true
                        },
                        onStopTimer = {
                            quickLogBookId = homeState.activeTimer?.bookId
                            isQuickLogOpen = true
                        }
                    )
                }

                // Library Route
                composable<LibraryRoute> {
                    val libraryViewModel = remember {
                        LibraryViewModel(
                            repository = container.repository,
                            statsCalculator = container.statsCalculator,
                            settingsRepository = container.settingsRepository
                        )
                    }
                    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()

                    LibraryScreen(
                        state = libraryState,
                        onFilterChange = { libraryViewModel.setFilter(it) },
                        onSortChange = { libraryViewModel.setSortOrder(it) },
                        onBookClick = { bookId ->
                            navController.navigate(BookDetailRoute(bookId))
                        },
                        onNavigateToAddBook = {
                            navController.navigate(AddBookRoute)
                        },
                        onNavigateToEditBook = { bookId ->
                            navController.navigate(EditBookRoute(bookId))
                        },
                        onTogglePause = { book ->
                            val nextStatus = if (book.status == BookStatus.PAUSED) BookStatus.READING else BookStatus.PAUSED
                            libraryViewModel.setStatus(book.id, nextStatus)
                        },
                        onMarkFinished = { book ->
                            libraryViewModel.markFinished(book)
                        },
                        onDeleteBook = { book ->
                            libraryViewModel.deleteBook(book)
                        }
                    )
                }

                // Stats Route (Stage B Full Screen)
                composable<StatsRoute> {
                    val statsViewModel = remember {
                        com.sniperoftdm.booktracker.ui.stats.StatsViewModel(
                            repository = container.repository,
                            statsCalculator = container.statsCalculator,
                            settingsRepository = container.settingsRepository
                        )
                    }
                    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()

                    com.sniperoftdm.booktracker.ui.stats.StatsScreen(
                        state = statsState,
                        onPeriodSelected = { statsViewModel.selectPeriod(it) },
                        onDayClick = { statsViewModel.selectDay(it) },
                        onDismissDayDetail = { statsViewModel.clearSelectedDay() },
                        onSetYearlyGoal = { statsViewModel.setYearlyGoal(it) }
                    )
                }

                // Settings Route (Stage B Full Screen)
                composable<SettingsRoute> {
                    val settingsViewModel = remember {
                        com.sniperoftdm.booktracker.ui.settings.SettingsViewModel(
                            settingsRepository = container.settingsRepository,
                            backupRepository = container.backupRepository,
                            database = container.database,
                            bookRepository = container.repository
                        )
                    }
                    val currentSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
                    val backupState by settingsViewModel.backupState.collectAsStateWithLifecycle()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    com.sniperoftdm.booktracker.ui.settings.SettingsScreen(
                        settings = currentSettings,
                        backupState = backupState,
                        onCountZeroDaysChange = { settingsViewModel.setCountZeroDays(it) },
                        onPaceWindowDaysChange = { settingsViewModel.setPaceWindowDays(it) },
                        onYearlyGoalChange = { settingsViewModel.setYearlyGoalBooks(it) },
                        onReminderEnabledChange = { settingsViewModel.setReminderEnabled(it) },
                        onReminderTimeChange = { h, m -> settingsViewModel.setReminderTime(h, m) },
                        onThemeModeChange = { settingsViewModel.setThemeMode(it) },
                        onUseDynamicColorChange = { settingsViewModel.setUseDynamicColor(it) },
                        onExportBackup = { settingsViewModel.exportBackup(it) },
                        onImportBackup = { settingsViewModel.importBackup(it) },
                        onResetBackupState = { settingsViewModel.resetBackupState() },
                        onClearAllData = { settingsViewModel.clearAllData() },
                        onSendTestNotification = { settingsViewModel.sendTestNotification(context) },
                        onNavigateToDebug = { navController.navigate(DebugRoute) },
                        onNavigateToGallery = { navController.navigate(ClayGalleryRoute) }
                    )
                }

                // Book Detail Route (Stage B Full Screen)
                composable<BookDetailRoute> { backStack ->
                    val route = backStack.toRoute<BookDetailRoute>()
                    val detailViewModel = remember(route.bookId) {
                        com.sniperoftdm.booktracker.ui.detail.BookDetailViewModel(
                            bookId = route.bookId,
                            repository = container.repository,
                            statsCalculator = container.statsCalculator,
                            settingsRepository = container.settingsRepository,
                            timerRepository = container.timerRepository
                        )
                    }
                    val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()

                    com.sniperoftdm.booktracker.ui.detail.BookDetailScreen(
                        state = detailState,
                        onBack = { navController.popBackStack() },
                        onNavigateToEditBook = { bookId -> navController.navigate(EditBookRoute(bookId)) },
                        onQuickLogClick = { bookId ->
                            quickLogBookId = bookId
                            isQuickLogOpen = true
                        },
                        onStartTimer = { detailViewModel.startTimer() },
                        onCancelTimer = { detailViewModel.cancelTimer() },
                        onTogglePause = {
                            val current = detailState.book
                            if (current != null) {
                                val nextStatus = if (current.status == BookStatus.PAUSED) BookStatus.READING else BookStatus.PAUSED
                                detailViewModel.setStatus(nextStatus)
                            }
                        },
                        onMarkFinished = { detailViewModel.markFinished() },
                        onDeleteBook = {
                            detailViewModel.deleteBook()
                            navController.popBackStack()
                        },
                        onEditSession = { detailViewModel.editSession(it) },
                        onDeleteSession = { detailViewModel.deleteSession(it) },
                        onUndoDeleteSession = { detailViewModel.undoDeleteSession() },
                        onAddNote = { page, text -> detailViewModel.addNote(page, text) },
                        onEditNote = { detailViewModel.editNote(it) },
                        onDeleteNote = { detailViewModel.deleteNote(it) },
                        onUndoDeleteNote = { detailViewModel.undoDeleteNote() }
                    )
                }

                // Add Book Route
                composable<AddBookRoute> {
                    val context = LocalContext.current
                    val addViewModel = remember {
                        AddEditBookViewModel(
                            repository = container.repository,
                            statsCalculator = container.statsCalculator,
                            validator = container.sessionValidator,
                            coverStorage = container.coverStorage,
                            bookLookupRepository = container.bookLookupRepository
                        )
                    }
                    val addState by addViewModel.uiState.collectAsStateWithLifecycle()

                    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                    val candidateJson by savedStateHandle?.getStateFlow<String?>("selected_candidate", null)?.collectAsStateWithLifecycle()
                        ?: remember { mutableStateOf(null) }

                    LaunchedEffect(candidateJson) {
                        candidateJson?.let { jsonStr ->
                            try {
                                val candidate = kotlinx.serialization.json.Json.decodeFromString<com.sniperoftdm.booktracker.domain.BookCandidate>(jsonStr)
                                addViewModel.applyCandidate(candidate)
                            } catch (_: Exception) {}
                            savedStateHandle?.remove<String>("selected_candidate")
                        }
                    }

                    LaunchedEffect(Unit) {
                        addViewModel.events.collect { event ->
                            when (event) {
                                is AddEditBookEvent.Saved -> navController.popBackStack()
                                is AddEditBookEvent.Error -> Unit
                            }
                        }
                    }

                    AddEditBookScreen(
                        state = addState,
                        onTitleChange = { addViewModel.onTitleChange(it) },
                        onAuthorChange = { addViewModel.onAuthorChange(it) },
                        onTotalPagesChange = { addViewModel.onTotalPagesChange(it) },
                        onStartPageChange = { addViewModel.onStartPageChange(it) },
                        onGoalDateChange = { addViewModel.onGoalDateChange(it) },
                        onStatusChange = { addViewModel.onStatusChange(it) },
                        onCoverSelected = { addViewModel.onCoverSelected(it) },
                        onRemoveCover = { addViewModel.onRemoveCover() },
                        onNavigateToSearch = { navController.navigate(SearchOnlineRoute) },
                        onIsbnScanned = { addViewModel.onIsbnScanned(it) },
                        onSave = { addViewModel.save(context) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Edit Book Route
                composable<EditBookRoute> { backStack ->
                    val context = LocalContext.current
                    val route = backStack.toRoute<EditBookRoute>()
                    val editViewModel = remember(route.bookId) {
                        AddEditBookViewModel(
                            repository = container.repository,
                            statsCalculator = container.statsCalculator,
                            validator = container.sessionValidator,
                            coverStorage = container.coverStorage,
                            bookId = route.bookId
                        )
                    }
                    val editState by editViewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        editViewModel.events.collect { event ->
                            when (event) {
                                is AddEditBookEvent.Saved -> navController.popBackStack()
                                is AddEditBookEvent.Error -> Unit
                            }
                        }
                    }

                    AddEditBookScreen(
                        state = editState,
                        onTitleChange = { editViewModel.onTitleChange(it) },
                        onAuthorChange = { editViewModel.onAuthorChange(it) },
                        onTotalPagesChange = { editViewModel.onTotalPagesChange(it) },
                        onStartPageChange = { editViewModel.onStartPageChange(it) },
                        onGoalDateChange = { editViewModel.onGoalDateChange(it) },
                        onStatusChange = { editViewModel.onStatusChange(it) },
                        onCoverSelected = { editViewModel.onCoverSelected(it) },
                        onRemoveCover = { editViewModel.onRemoveCover() },
                        onSave = { editViewModel.save(context) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Clay Gallery Route (Debug)
                composable<ClayGalleryRoute> {
                    ClayGalleryScreen(onBack = { navController.popBackStack() })
                }

                // Legacy Debug Screen Route
                composable<DebugRoute> {
                    DebugScreen(container = container)
                }

                // Flavor Routes
                with(FlavorUi) {
                    flavorRoutes(navController = navController, appContainer = container)
                }
            }

            // Floating Navigation Bar (hides on sub-screens)
            AnimatedVisibility(
                visible = isTopLevelRoute,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                ClayNavBar(
                    items = navItems,
                    currentRoute = currentNavItem,
                    onItemClick = { item ->
                        val targetRoute = item.route
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Quick Log Modal Bottom Sheet
            if (isQuickLogOpen) {
                val quickLogVm = remember(quickLogBookId) {
                    QuickLogViewModel(
                        repository = container.repository,
                        timerRepository = container.timerRepository
                    ).apply {
                        if (quickLogBookId != null) selectBook(quickLogBookId!!)
                    }
                }

                QuickLogBottomSheet(
                    viewModel = quickLogVm,
                    onDismiss = { isQuickLogOpen = false },
                    onSaved = { pagesAdded, sessionId, bookId, isFinished, book ->
                        isQuickLogOpen = false
                        lastSavedInfo = sessionId to bookId
                        snackbarMessage = "Saved +$pagesAdded pages"

                        if (isFinished && book != null) {
                            finishedBookDialogData = book
                        }
                    }
                )
            }

            // Undo Snackbar
            if (snackbarMessage != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isTopLevelRoute) 88.dp else 24.dp)
                ) {
                    ClaySnackbar(
                        message = snackbarMessage!!,
                        actionLabel = "UNDO",
                        onActionClick = {
                            val saved = lastSavedInfo
                            if (saved != null) {
                                scope.launch {
                                    val sessions = container.repository.observeSessions(saved.second).first()
                                    val session = sessions.find { it.id == saved.first }
                                    if (session != null) {
                                        container.repository.deleteSession(session)
                                    }
                                }
                            }
                            snackbarMessage = null
                        }
                    )
                }
                LaunchedEffect(snackbarMessage) {
                    kotlinx.coroutines.delay(4000)
                    snackbarMessage = null
                }
            }

            // Book Finished Congratulatory ClayDialog
            if (finishedBookDialogData != null) {
                val book = finishedBookDialogData!!
                ClayDialog(
                    onDismissRequest = { finishedBookDialogData = null }
                ) {
                    Text(
                        text = "Book Finished!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Congratulations! You finished reading ${book.title}.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = Dimens.spaceSmall)
                    )
                    Spacer(modifier = Modifier.height(Dimens.spaceLarge))
                    ClayButton(
                        onClick = {
                            finishedBookDialogData = null
                            navController.navigate(BookDetailRoute(book.id))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Book Details")
                    }
                }
            }
        }
    }
}
