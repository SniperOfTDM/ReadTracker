package com.sniperoftdm.booktracker.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.BuildConfig
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.repository.ThemeMode
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayChip
import com.sniperoftdm.booktracker.ui.clay.ClayDialog
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClayIconButton
import com.sniperoftdm.booktracker.ui.clay.ClayScreenScaffold
import com.sniperoftdm.booktracker.ui.clay.ClaySegmentedControl
import com.sniperoftdm.booktracker.ui.clay.ClaySlider
import com.sniperoftdm.booktracker.ui.clay.ClaySurface
import com.sniperoftdm.booktracker.ui.clay.ClaySwitch
import com.sniperoftdm.booktracker.ui.components.ConfirmDialog
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    backupState: BackupOperationState,
    onCountZeroDaysChange: (Boolean) -> Unit,
    onPaceWindowDaysChange: (Int) -> Unit,
    onYearlyGoalChange: (Int) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onUseDynamicColorChange: (Boolean) -> Unit,
    onExportBackup: (OutputStream) -> Unit,
    onImportBackup: (InputStream) -> Unit,
    onResetBackupState: () -> Unit,
    onClearAllData: () -> Unit,
    onSendTestNotification: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // SAF Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                onExportBackup(stream)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirmDialog = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onReminderEnabledChange(true)
        } else {
            showPermissionRationaleDialog = true
        }
    }

    ClayScreenScaffold(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        isTopLevel = true
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Dimens.contentMaxWidth),
            contentPadding = PaddingValues(
                start = Dimens.spaceLarge,
                end = Dimens.spaceLarge,
                top = Dimens.spaceSmall,
                bottom = paddingValues.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge)
        ) {
            // SECTION 1: READING PREFERENCES
            item(key = "section_reading") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                    SectionHeading(
                        title = stringResource(R.string.section_reading),
                        icon = Icons.Rounded.AutoStories,
                        accentColor = clayColors.sage
                    )

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
                    ) {
                        Column {
                            // Row 1: Count zero days
                            SettingsRow(
                                icon = Icons.Rounded.CalendarToday,
                                title = stringResource(R.string.setting_count_zero_days),
                                subtitle = "Include days without reading in pace averages",
                                trailing = {
                                    ClaySwitch(
                                        checked = settings.countZeroDays,
                                        onCheckedChange = onCountZeroDaysChange
                                    )
                                }
                            )

                            SettingsDivider(color = clayColors.edge.copy(alpha = 0.2f))

                            // Row 2: Pace window
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.spaceSmall)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Dimens.spaceMedium))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.setting_pace_window_title),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Recent period used to estimate reading pace",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    ClaySurface(
                                        shape = ClayShapes.chip,
                                        color = clayColors.sage,
                                        elevation = ClayElevation.Raised,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = pluralStringResource(
                                                R.plurals.days_count,
                                                settings.paceWindowDays,
                                                settings.paceWindowDays
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = clayColors.textOnAccent(clayColors.sage)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Dimens.spaceSmall))

                                ClaySlider(
                                    value = settings.paceWindowDays.toFloat(),
                                    onValueChange = { onPaceWindowDaysChange(it.toInt()) },
                                    valueRange = 3f..30f,
                                    steps = 26,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: GOALS
            item(key = "section_goals") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                    SectionHeading(
                        title = stringResource(R.string.section_goals),
                        icon = Icons.Rounded.EmojiEvents,
                        accentColor = clayColors.peach
                    )

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
                    ) {
                        SettingsRow(
                            icon = Icons.Rounded.Flag,
                            title = stringResource(R.string.setting_yearly_goal_title),
                            subtitle = if (settings.yearlyGoalBooks > 0) {
                                pluralStringResource(
                                    R.plurals.yearly_goal_books,
                                    settings.yearlyGoalBooks,
                                    settings.yearlyGoalBooks
                                )
                            } else {
                                stringResource(R.string.setting_yearly_goal_off)
                            },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
                                ) {
                                    ClayIconButton(
                                        onClick = {
                                            onYearlyGoalChange((settings.yearlyGoalBooks - 1).coerceAtLeast(0))
                                        },
                                        enabled = settings.yearlyGoalBooks > 0,
                                        elevation = ClayElevation.Raised,
                                        contentDescription = "Decrease yearly goal"
                                    ) {
                                        Icon(Icons.Rounded.Remove, contentDescription = null)
                                    }

                                    Text(
                                        text = if (settings.yearlyGoalBooks > 0) "${settings.yearlyGoalBooks}" else "Off",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.widthIn(min = 36.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    ClayIconButton(
                                        onClick = {
                                            onYearlyGoalChange(settings.yearlyGoalBooks + 1)
                                        },
                                        elevation = ClayElevation.Raised,
                                        contentDescription = "Increase yearly goal"
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // SECTION 3: DAILY REMINDER
            item(key = "section_reminder") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                    SectionHeading(
                        title = stringResource(R.string.section_reminder),
                        icon = Icons.Rounded.Notifications,
                        accentColor = clayColors.butter
                    )

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
                    ) {
                        Column {
                            // Switch row
                            SettingsRow(
                                icon = Icons.Rounded.Notifications,
                                title = stringResource(R.string.setting_reminder_enabled),
                                subtitle = "Receive a daily notification if you haven't read yet",
                                trailing = {
                                    ClaySwitch(
                                        checked = settings.reminderEnabled,
                                        onCheckedChange = { enable ->
                                            if (enable) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    onReminderEnabledChange(true)
                                                }
                                            } else {
                                                onReminderEnabledChange(false)
                                            }
                                        }
                                    )
                                }
                            )

                            if (settings.reminderEnabled) {
                                SettingsDivider(color = clayColors.edge.copy(alpha = 0.2f))

                                val formattedTime = String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute)

                                // U7: Reminder time row with trailing chip opening clay time picker dialog
                                SettingsRow(
                                    icon = Icons.Rounded.Schedule,
                                    title = stringResource(R.string.setting_reminder_time_title),
                                    subtitle = "Tap to change notification schedule",
                                    onClick = { showTimePickerDialog = true },
                                    trailing = {
                                        ClayChip(
                                            selected = false,
                                            onClick = { showTimePickerDialog = true },
                                            inactiveColor = clayColors.butter,
                                            label = {
                                                Text(
                                                    text = formattedTime,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = clayColors.textOnAccent(clayColors.butter)
                                                )
                                            }
                                        )
                                    }
                                )

                                if (BuildConfig.DEBUG) {
                                    SettingsDivider(color = clayColors.edge.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(Dimens.spaceSmall))
                                    ClayButton(
                                        onClick = onSendTestNotification,
                                        variant = ClayButtonVariant.OutlinedInset,
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text(stringResource(R.string.send_test_notification))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: APPEARANCE
            item(key = "section_appearance") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                    SectionHeading(
                        title = stringResource(R.string.section_appearance),
                        icon = Icons.Rounded.Palette,
                        accentColor = clayColors.lilac
                    )

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
                    ) {
                        Column {
                            val themeItems = listOf(
                                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                                ThemeMode.DARK to stringResource(R.string.theme_dark)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.spaceMedium))
                                Text(
                                    text = "Theme Mode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            ClaySegmentedControl(
                                items = themeItems,
                                selectedIndex = themeItems.indexOfFirst { it.first == settings.themeMode }.coerceAtLeast(0),
                                onItemSelected = { index -> onThemeModeChange(themeItems[index].first) },
                                modifier = Modifier.fillMaxWidth()
                            ) { (_, label) ->
                                Text(label, fontWeight = FontWeight.SemiBold)
                            }

                            // Dynamic color style for Android 12+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                SettingsDivider(color = clayColors.edge.copy(alpha = 0.2f))

                                SettingsRow(
                                    icon = Icons.Rounded.ColorLens,
                                    title = stringResource(R.string.setting_color_style),
                                    subtitle = "Material You dynamic colors from wallpaper",
                                    trailing = {
                                        ClaySwitch(
                                            checked = settings.useDynamicColor,
                                            onCheckedChange = onUseDynamicColorChange
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 5: DATA & BACKUP
            item(key = "section_data") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                    SectionHeading(
                        title = stringResource(R.string.section_data),
                        icon = Icons.Rounded.Storage,
                        accentColor = clayColors.sky
                    )

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.spaceMedium))
                                Column {
                                    Text(
                                        text = "Backup & Restore",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Export and import your reading library as local JSON",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)
                            ) {
                                ClayButton(
                                    onClick = {
                                        exportLauncher.launch("BookTracker_Backup_${System.currentTimeMillis()}.json")
                                    },
                                    variant = ClayButtonVariant.Filled,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(stringResource(R.string.action_export_json))
                                }

                                ClayButton(
                                    onClick = {
                                        importLauncher.launch(arrayOf("application/json"))
                                    },
                                    variant = ClayButtonVariant.OutlinedInset,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(stringResource(R.string.action_import_json))
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 6: ABOUT
            item(key = "section_about") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                    SectionHeading(
                        title = stringResource(R.string.section_about),
                        icon = Icons.Rounded.Info,
                        accentColor = clayColors.sage
                    )

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = BookTrackerTypography.bookTitleLarge,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.app_version_format,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Build flavor row
                            Text(
                                text = if (BuildConfig.HAS_ONLINE_LOOKUP) {
                                    stringResource(R.string.about_build_online)
                                } else {
                                    stringResource(R.string.about_build_offline)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (BuildConfig.HAS_ONLINE_LOOKUP) clayColors.sage else clayColors.terracotta,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (BuildConfig.HAS_ONLINE_LOOKUP) {
                                    stringResource(R.string.privacy_statement_online)
                                } else {
                                    stringResource(R.string.privacy_statement_offline)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            com.sniperoftdm.booktracker.flavor.FlavorUi.RedownloadCoversButton()
                            Spacer(modifier = Modifier.height(4.dp))
                            ClayButton(
                                onClick = { showLicensesDialog = true },
                                variant = ClayButtonVariant.Tonal,
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text(stringResource(R.string.source_licenses))
                            }
                        }
                    }
                }
            }

            // SECTION 7: DEVELOPER (BuildConfig.DEBUG only)
            if (BuildConfig.DEBUG) {
                item(key = "section_developer") {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                        SectionHeading(
                            title = stringResource(R.string.section_developer),
                            icon = Icons.Rounded.BugReport,
                            accentColor = clayColors.terracotta
                        )

                        ClayCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = ClayElevation.Raised,
                            color = clayColors.terracotta.copy(alpha = 0.08f)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)) {
                                ClayButton(
                                    onClick = onNavigateToGallery,
                                    variant = ClayButtonVariant.Tonal,
                                    leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(stringResource(R.string.dev_gallery))
                                }

                                ClayButton(
                                    onClick = onNavigateToDebug,
                                    variant = ClayButtonVariant.OutlinedInset,
                                    leadingIcon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(stringResource(R.string.dev_old_debug))
                                }

                                ClayButton(
                                    onClick = {
                                        scope.launch {
                                            feedbackMessage = "Demo data seeded"
                                        }
                                    },
                                    variant = ClayButtonVariant.Tonal,
                                    leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(stringResource(R.string.dev_seed_data))
                                }

                                ClayButton(
                                    onClick = { showClearConfirmDialog = true },
                                    variant = ClayButtonVariant.OutlinedInset,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.dev_clear_data),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                if (feedbackMessage != null) {
                                    Text(
                                        text = feedbackMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = clayColors.sage
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom anchor with testTag for Compose automated testing
            item(key = "settings_last_item") {
                Box(modifier = Modifier.testTag("settings_last_item")) {
                    Spacer(modifier = Modifier.height(Dimens.spaceSmall))
                }
            }
        }
    }

    // U7: Clay TimePicker Dialog
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = true
        )

        ClayDialog(onDismissRequest = { showTimePickerDialog = false }) {
            Text(
                text = stringResource(R.string.setting_reminder_time_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimens.spaceMedium))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
            Spacer(modifier = Modifier.height(Dimens.spaceLarge))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ClayButton(
                    onClick = { showTimePickerDialog = false },
                    variant = ClayButtonVariant.OutlinedInset,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                ClayButton(
                    onClick = {
                        onReminderTimeChange(timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    },
                    variant = ClayButtonVariant.Filled,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    // Import Confirmation Dialog
    if (showImportConfirmDialog && pendingImportUri != null) {
        ConfirmDialog(
            title = stringResource(R.string.import_confirm_title),
            message = stringResource(R.string.import_confirm_message),
            confirmText = stringResource(R.string.action_confirm),
            isDestructive = true,
            onConfirm = {
                val uri = pendingImportUri
                showImportConfirmDialog = false
                pendingImportUri = null
                if (uri != null) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        onImportBackup(stream)
                    }
                }
            },
            onDismiss = {
                showImportConfirmDialog = false
                pendingImportUri = null
            }
        )
    }

    // Clear All Data Confirmation Dialog
    if (showClearConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dev_clear_confirm_title),
            message = stringResource(R.string.dev_clear_confirm_message),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                showClearConfirmDialog = false
                onClearAllData()
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }

    // Licenses Dialog
    if (showLicensesDialog) {
        ClayDialog(onDismissRequest = { showLicensesDialog = false }) {
            Text(
                text = stringResource(R.string.source_licenses),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimens.spaceMedium))
            Text(
                text = "• Jetpack Compose (Apache 2.0)\n• AndroidX Room (Apache 2.0)\n• Kotlinx Coroutines & Serialization (Apache 2.0)\n• AndroidX DataStore & WorkManager (Apache 2.0)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.spaceLarge))
            ClayButton(
                onClick = { showLicensesDialog = false },
                variant = ClayButtonVariant.Filled,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("OK")
            }
        }
    }

    // Notification Permission Rationale Dialog
    if (showPermissionRationaleDialog) {
        ClayDialog(onDismissRequest = { showPermissionRationaleDialog = false }) {
            Text(
                text = "Notification Permission",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimens.spaceMedium))
            Text(
                text = stringResource(R.string.permission_notification_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.spaceLarge))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ClayButton(
                    onClick = { showPermissionRationaleDialog = false },
                    variant = ClayButtonVariant.OutlinedInset,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                ClayButton(
                    onClick = {
                        showPermissionRationaleDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    variant = ClayButtonVariant.Filled,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        }
    }

    // Backup Operation Result Dialogs
    when (backupState) {
        is BackupOperationState.ExportSuccess -> {
            ClayDialog(onDismissRequest = onResetBackupState) {
                Text("Export Complete", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                Text("Your backup JSON file was exported successfully.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(Dimens.spaceLarge))
                ClayButton(onClick = onResetBackupState, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("OK")
                }
            }
        }
        is BackupOperationState.ExportError -> {
            ClayDialog(onDismissRequest = onResetBackupState) {
                Text("Export Failed", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                Text(backupState.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(Dimens.spaceLarge))
                ClayButton(onClick = onResetBackupState, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("OK")
                }
            }
        }
        is BackupOperationState.ImportSuccess -> {
            ClayDialog(onDismissRequest = onResetBackupState) {
                Text(stringResource(R.string.import_success_title), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                val booksStr = pluralStringResource(R.plurals.import_success_books, backupState.books, backupState.books)
                val sessionsStr = pluralStringResource(R.plurals.import_success_sessions, backupState.sessions, backupState.sessions)
                val notesStr = pluralStringResource(R.plurals.import_success_notes, backupState.notes, backupState.notes)
                Text(
                    text = stringResource(R.string.import_success_summary, booksStr, sessionsStr, notesStr),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Dimens.spaceLarge))
                ClayButton(onClick = onResetBackupState, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("OK")
                }
            }
        }
        is BackupOperationState.ImportValidationFailed -> {
            ClayDialog(onDismissRequest = onResetBackupState) {
                Text(
                    stringResource(R.string.import_error_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                Text("The imported backup contains conflicting records and was not applied:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(Dimens.spaceSmall))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    items(backupState.report.problems) { problem ->
                        Text("• ${problem.bookTitle}: ${problem.reason}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.spaceLarge))
                ClayButton(onClick = onResetBackupState, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("OK")
                }
            }
        }
        is BackupOperationState.ImportError -> {
            ClayDialog(onDismissRequest = onResetBackupState) {
                Text("Import Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                Text(backupState.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(Dimens.spaceLarge))
                ClayButton(onClick = onResetBackupState, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("OK")
                }
            }
        }
        is BackupOperationState.Idle -> Unit
    }
}

@Composable
private fun SectionHeading(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
    ) {
        ClaySurface(
            shape = ClayShapes.chip,
            color = accentColor,
            elevation = ClayElevation.Raised,
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = clayColors.textOnAccent(accentColor)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Dimens.spaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.spaceMedium))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimens.spaceSmall))
        trailing()
    }
}

@Composable
private fun SettingsDivider(
    color: Color,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = Dimens.spaceMedium),
        color = color
    )
}
