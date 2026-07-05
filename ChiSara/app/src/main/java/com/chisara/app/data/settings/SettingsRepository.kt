package com.chisara.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chisara.app.notification.NotificationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chisara_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            trackedPackages = prefs[KEY_TRACKED] ?: NotificationConfig.trackedPackages,
            penaltyEnabled = prefs[KEY_PENALTY_ENABLED] ?: false,
            wrongAnswerPenalty = prefs[KEY_PENALTY_VALUE] ?: DEFAULT_PENALTY
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setPackageTracked(pkg: String, tracked: Boolean) {
        store.edit { prefs ->
            val currentSet = prefs[KEY_TRACKED] ?: NotificationConfig.trackedPackages
            prefs[KEY_TRACKED] = if (tracked) currentSet + pkg else currentSet - pkg
        }
    }

    suspend fun setPenaltyEnabled(enabled: Boolean) {
        store.edit { it[KEY_PENALTY_ENABLED] = enabled }
    }

    suspend fun setPenaltyValue(value: Int) {
        store.edit { it[KEY_PENALTY_VALUE] = value.coerceIn(0, 100) }
    }

    private companion object {
        val KEY_TRACKED = stringSetPreferencesKey("tracked_packages")
        val KEY_PENALTY_ENABLED = booleanPreferencesKey("penalty_enabled")
        val KEY_PENALTY_VALUE = intPreferencesKey("penalty_value")
        const val DEFAULT_PENALTY = 5
    }
}
