package com.dd.sms.hook.features.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import javax.inject.Inject

private const val TAG = "SettingsRepository"

private object Keys {
    val FORWARDING: Preferences.Key<Boolean> = booleanPreferencesKey("forwarding_enabled")
    val KEEP_ALIVE: Preferences.Key<Boolean> = booleanPreferencesKey("keep_alive_enabled")
    val NOTIFY_FAILURE: Preferences.Key<Boolean> = booleanPreferencesKey("notify_on_failure")
    val RETENTION: Preferences.Key<String> = stringPreferencesKey("retention")
    val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR: Preferences.Key<Boolean> = booleanPreferencesKey("dynamic_color")
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            AppLogger.e(TAG, "read settings failed - falling back to defaults", error)
            emit(emptyPreferences())
        }
        .map { prefs -> toSettings(prefs) }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setForwardingEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.FORWARDING] = enabled }
    }

    override suspend fun setKeepAliveEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_ALIVE] = enabled }
    }

    override suspend fun setNotifyOnFailure(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFY_FAILURE] = enabled }
    }

    override suspend fun setRetention(retention: RetentionPeriod) {
        dataStore.edit { it[Keys.RETENTION] = retention.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    private fun toSettings(prefs: Preferences): AppSettings {
        val defaults: AppSettings = AppSettings.DEFAULT

        return AppSettings(
            forwardingEnabled = prefs[Keys.FORWARDING] ?: defaults.forwardingEnabled,
            keepAliveEnabled = prefs[Keys.KEEP_ALIVE] ?: defaults.keepAliveEnabled,
            notifyOnFailure = prefs[Keys.NOTIFY_FAILURE] ?: defaults.notifyOnFailure,
            retention = RetentionPeriod.entries.firstOrNull { it.name == prefs[Keys.RETENTION] }
                ?: defaults.retention,
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] } ?: defaults.themeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
        )
    }
}
