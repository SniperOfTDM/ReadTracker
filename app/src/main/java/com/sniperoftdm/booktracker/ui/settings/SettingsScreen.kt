package com.sniperoftdm.booktracker.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sniperoftdm.booktracker.BuildConfig
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.data.repository.ThemeMode
import com.sniperoftdm.booktracker.data.repository.UserSettings
import com.sniperoftdm.booktracker.ui.clay.ClayButton
import com.sniperoftdm.booktracker.ui.clay.ClayButtonVariant
import com.sniperoftdm.booktracker.ui.clay.ClayCard
import com.sniperoftdm.booktracker.ui.clay.ClayDialog
import com.sniperoftdm.booktracker.ui.clay.ClayElevation
import com.sniperoftdm.booktracker.ui.clay.ClaySegmentedControl
import com.sniperoftdm.booktracker.ui.clay.ClaySlider
import com.sniperoftdm.booktracker.ui.clay.ClaySwitch
import com.sniperoftdm.booktracker.ui.components.ConfirmDialog
import com.sniperoftdm.booktracker.ui.components.PageStepper
import com.sniperoftdm.booktracker.ui.components.TimePickerField
import com.sniperoftdm.booktracker.ui.debug.SeedData
import com.sniperoftdm.booktracker.ui.theme.BookTrackerTypography
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalTime

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

    var showGoalStepperDialog by remember { mutableStateOf(false) }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Dimens.contentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spaceLarge, vertical = Dimens.spaceMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge)
        ) {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            // SECTION 1: READING
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                Text(
                    text = stringResource(R.string.section_reading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ClayElevation.Raised,
                    color = clayColors.face
                ) {
                    // Count zero days switch row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_count_zero_days),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Include days without reading in pace averages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ClaySwitch(
                            checked = settings.countZeroDays,
                            onCheckedChange = onCountZeroDaysChange
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                    HorizontalDivider(color = clayColors.edge.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                    // Pace window slider row
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.setting_pace_window, settings.paceWindowDays),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        ClaySlider(
                            value = settings.paceWindowDays.toFloat(),
                            onValueChange = { onPaceWindowDaysChange(it.toInt()) },
                            valueRange = 3f..30f,
                            steps = 26
                        )
                    }
                }
            }

            // SECTION 2: GOALS
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                Text(
                    text = stringResource(R.string.section_goals),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ClayElevation.Raised,
                    color = clayColors.face,
                    onClick = { showGoalStepperDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (settings.yearlyGoalBooks > 0) {
                                    stringResource(R.string.setting_yearly_goal, settings.yearlyGoalBooks)
                                } else {
                                    stringResource(R.string.setting_yearly_goal_off)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap to change annual target",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ClayButton(
                            onClick = { showGoalStepperDialog = true },
                            variant = ClayButtonVariant.Tonal,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Edit")
                        }
                    }
                }
            }

            // SECTION 3: DAILY REMINDER
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                Text(
                    text = stringResource(R.string.section_reminder),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ClayElevation.Raised,
                    color = clayColors.face
                ) {
                    // Switch row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_reminder_enabled),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Receive a daily notification if you haven't read yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

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

                    if (settings.reminderEnabled) {
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                        HorizontalDivider(color = clayColors.edge.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                        TimePickerField(
                            selectedTime = LocalTime.of(settings.reminderHour, settings.reminderMinute),
                            onTimeSelected = { time ->
                                onReminderTimeChange(time.hour, time.minute)
                            },
                            label = stringResource(
                                R.string.setting_reminder_time,
                                settings.reminderHour,
                                settings.reminderMinute
                            )
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                        ClayButton(
                            onClick = onSendTestNotification,
                            variant = ClayButtonVariant.OutlinedInset,
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text(stringResource(R.string.send_test_notification))
                        }
                    }
                }
            }

            // SECTION 4: APPEARANCE
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                Text(
                    text = stringResource(R.string.section_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ClayElevation.Raised,
                    color = clayColors.face
                ) {
                    val themeItems = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark)
                    )

                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(Dimens.spaceSmall))

                    ClaySegmentedControl(
                        items = themeItems,
                        selectedIndex = themeItems.indexOfFirst { it.first == settings.themeMode }.coerceAtLeast(0),
                        onItemSelected = { index -> onThemeModeChange(themeItems[index].first) }
                    ) { (_, label) ->
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }

                    // Dynamic color style for Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                        HorizontalDivider(color = clayColors.edge.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.setting_color_style),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Material You dynamic colors from wallpaper",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            ClaySwitch(
                                checked = settings.useDynamicColor,
                                onCheckedChange = onUseDynamicColorChange
                            )
                        }
                    }
                }
            }

            // SECTION 5: DATA & BACKUP
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                Text(
                    text = stringResource(R.string.section_data),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ClayElevation.Raised,
                    color = clayColors.face
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)) {
                        ClayButton(
                            onClick = {
                                exportLauncher.launch("BookTracker_Backup_${System.currentTimeMillis()}.json")
                            },
                            variant = ClayButtonVariant.Filled,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(stringResource(R.string.action_export_json))
                        }

                        ClayButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            variant = ClayButtonVariant.OutlinedInset,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(stringResource(R.string.action_import_json))
                        }
                    }
                }
            }

            // SECTION 6: ABOUT
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                Text(
                    text = stringResource(R.string.section_about),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_version_format, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.privacy_statement),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ClayButton(
                            onClick = { showLicensesDialog = true },
                            variant = ClayButtonVariant.Tonal,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text(stringResource(R.string.source_licenses))
                        }
                    }
                }
            }

            // SECTION 7: DEVELOPER (BuildConfig.DEBUG only)
            if (BuildConfig.DEBUG) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
                    Text(
                        text = stringResource(R.string.section_developer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ClayElevation.Raised,
                        color = clayColors.face
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
                                        // We can seed demo data
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

            Spacer(modifier = Modifier.height(Dimens.spaceHuge))
        }

        // Stepper Dialog for Yearly Goal
        if (showGoalStepperDialog) {
            var goalInput by remember { mutableIntStateOf(settings.yearlyGoalBooks) }

            ClayDialog(onDismissRequest = { showGoalStepperDialog = false }) {
                Text(
                    text = stringResource(R.string.yearly_goal_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                Text(
                    text = "Set your annual target in number of books (0 = disabled):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                PageStepper(
                    page = goalInput,
                    totalPages = 365,
                    onPageChange = { goalInput = it.coerceAtLeast(0) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Dimens.spaceLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ClayButton(
                        onClick = { showGoalStepperDialog = false },
                        variant = ClayButtonVariant.OutlinedInset,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(Dimens.spaceSmall))

                    ClayButton(
                        onClick = {
                            onYearlyGoalChange(goalInput)
                            showGoalStepperDialog = false
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
                    Text(
                        stringResource(
                            R.string.import_success_message,
                            backupState.books,
                            backupState.sessions,
                            backupState.notes
                        ),
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
}
