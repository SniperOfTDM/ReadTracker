package com.sniperoftdm.booktracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val COUNT_ZERO_DAYS = booleanPreferencesKey("count_zero_days")
        val PACE_WINDOW_DAYS = intPreferencesKey("pace_window_days")
        val YEARLY_GOAL_BOOKS = intPreferencesKey("yearly_goal_books")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    }

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE]
            val themeMode = try {
                if (themeModeStr != null) ThemeMode.valueOf(themeModeStr) else ThemeMode.SYSTEM
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
            UserSettings(
                countZeroDays = preferences[PreferencesKeys.COUNT_ZERO_DAYS] ?: true,
                paceWindowDays = (preferences[PreferencesKeys.PACE_WINDOW_DAYS] ?: 7).coerceIn(3, 30),
                yearlyGoalBooks = (preferences[PreferencesKeys.YEARLY_GOAL_BOOKS] ?: 0).coerceAtLeast(0),
                reminderEnabled = preferences[PreferencesKeys.REMINDER_ENABLED] ?: false,
                reminderHour = (preferences[PreferencesKeys.REMINDER_HOUR] ?: 21).coerceIn(0, 23),
                reminderMinute = (preferences[PreferencesKeys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
                themeMode = themeMode,
                useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: false
            )
        }

    override suspend fun setCountZeroDays(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COUNT_ZERO_DAYS] = enabled
        }
    }

    override suspend fun setPaceWindowDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PACE_WINDOW_DAYS] = days.coerceIn(3, 30)
        }
    }

    override suspend fun setYearlyGoalBooks(goal: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.YEARLY_GOAL_BOOKS] = goal.coerceAtLeast(0)
        }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[PreferencesKeys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun updateSettings(settings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COUNT_ZERO_DAYS] = settings.countZeroDays
            preferences[PreferencesKeys.PACE_WINDOW_DAYS] = settings.paceWindowDays.coerceIn(3, 30)
            preferences[PreferencesKeys.YEARLY_GOAL_BOOKS] = settings.yearlyGoalBooks.coerceAtLeast(0)
            preferences[PreferencesKeys.REMINDER_ENABLED] = settings.reminderEnabled
            preferences[PreferencesKeys.REMINDER_HOUR] = settings.reminderHour.coerceIn(0, 23)
            preferences[PreferencesKeys.REMINDER_MINUTE] = settings.reminderMinute.coerceIn(0, 59)
            preferences[PreferencesKeys.THEME_MODE] = settings.themeMode.name
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = settings.useDynamicColor
        }
    }
}
