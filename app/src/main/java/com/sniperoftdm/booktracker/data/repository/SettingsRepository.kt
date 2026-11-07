package com.sniperoftdm.booktracker.data.repository

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Immutable user preferences and calculation configurations.
 */
data class UserSettings(
    val countZeroDays: Boolean = true,
    val paceWindowDays: Int = 7,
    val yearlyGoalBooks: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false
)

interface SettingsRepository {

    /** Observable flow of current user settings. */
    val settings: Flow<UserSettings>

    /** Updates whether days with zero pages count toward daily reading averages. */
    suspend fun setCountZeroDays(enabled: Boolean)

    /** Updates the rolling pace window in days, constrained to between 3 and 30. */
    suspend fun setPaceWindowDays(days: Int)

    /** Sets the annual reading target in total books (0 disables goal tracking). */
    suspend fun setYearlyGoalBooks(goal: Int)

    /** Enables or disables the scheduled daily reading reminder. */
    suspend fun setReminderEnabled(enabled: Boolean)

    /** Configures the local time of day for the daily reminder notification. */
    suspend fun setReminderTime(hour: Int, minute: Int)

    /** Updates the theme mode (System, Light, Dark). */
    suspend fun setThemeMode(mode: ThemeMode)

    /** Updates whether dynamic wallpaper coloring is used on Android 12+. */
    suspend fun setUseDynamicColor(enabled: Boolean)

    /** Replaces all settings at once (e.g., during backup restore). */
    suspend fun updateSettings(settings: UserSettings)
}
