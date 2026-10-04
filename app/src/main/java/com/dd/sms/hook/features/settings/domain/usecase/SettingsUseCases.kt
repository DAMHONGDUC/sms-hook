package com.dd.sms.hook.features.settings.domain.usecase

import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val TAG = "SettingsUseCases"

class ObserveSettingsUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}

/** One entry point per setting keeps every change logged with its new value. */
class UpdateSettingsUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend fun forwarding(enabled: Boolean) {
        repository.setForwardingEnabled(enabled)
        AppLogger.i(TAG, "forwarding changed - {enabled: $enabled}")
    }

    suspend fun keepAlive(enabled: Boolean) {
        repository.setKeepAliveEnabled(enabled)
        AppLogger.i(TAG, "keep-alive changed - {enabled: $enabled}")
    }

    suspend fun notifyOnFailure(enabled: Boolean) {
        repository.setNotifyOnFailure(enabled)
        AppLogger.i(TAG, "notify on failure changed - {enabled: $enabled}")
    }

    suspend fun retention(retention: RetentionPeriod) {
        repository.setRetention(retention)
        AppLogger.i(TAG, "retention changed - {retention: $retention}")
    }

    suspend fun themeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
        AppLogger.i(TAG, "theme mode changed - {mode: $mode}")
    }

    suspend fun dynamicColor(enabled: Boolean) {
        repository.setDynamicColor(enabled)
        AppLogger.i(TAG, "dynamic color changed - {enabled: $enabled}")
    }
}
