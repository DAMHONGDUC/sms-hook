package com.dd.sms.hook.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.dispatch.domain.service.KeepAliveController
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import com.dd.sms.hook.features.settings.domain.usecase.ObserveSettingsUseCase
import com.dd.sms.hook.features.settings.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"
private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val keepAliveController: KeepAliveController,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onForwardingChange(enabled: Boolean) = launchLogged("forwarding") { updateSettings.forwarding(enabled) }

    fun onNotifyOnFailureChange(enabled: Boolean) = launchLogged("notify") { updateSettings.notifyOnFailure(enabled) }

    fun onRetentionChange(retention: RetentionPeriod) = launchLogged("retention") { updateSettings.retention(retention) }

    fun onThemeModeChange(mode: ThemeMode) = launchLogged("theme") { updateSettings.themeMode(mode) }

    fun onDynamicColorChange(enabled: Boolean) = launchLogged("dynamic color") { updateSettings.dynamicColor(enabled) }

    fun onKeepAliveChange(enabled: Boolean) = launchLogged("keep-alive") {
        updateSettings.keepAlive(enabled)
        if (enabled) keepAliveController.start() else keepAliveController.stop()
    }

    private fun launchLogged(setting: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                AppLogger.e(TAG, "updating setting failed - {setting: $setting}", e)
            }
        }
    }
}
