package com.dd.sms.hook.features.settings.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.shared.presentation.ui.UiMessage
import com.dd.sms.hook.features.devtools.domain.model.DemoSeedResult
import com.dd.sms.hook.features.devtools.domain.usecase.ClearDemoDataUseCase
import com.dd.sms.hook.features.devtools.domain.usecase.SeedDemoDataUseCase
import com.dd.sms.hook.features.dispatch.domain.service.KeepAliveController
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import com.dd.sms.hook.features.settings.domain.usecase.ObserveSettingsUseCase
import com.dd.sms.hook.features.settings.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val seedDemoData: SeedDemoDataUseCase,
    private val clearDemoData: ClearDemoDataUseCase,
) : ViewModel() {
    private val mutableDemoBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val messageChannel: Channel<UiMessage> = Channel(Channel.BUFFERED)

    val settings: StateFlow<AppSettings?> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
    /** True while demo data is being added or removed, so the buttons cannot be tapped twice. */
    val demoBusy: StateFlow<Boolean> = mutableDemoBusy.asStateFlow()
    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()

    fun onForwardingChange(enabled: Boolean) = launchLogged("forwarding") { updateSettings.forwarding(enabled) }

    fun onNotifyOnFailureChange(enabled: Boolean) = launchLogged("notify") { updateSettings.notifyOnFailure(enabled) }

    fun onRetentionChange(retention: RetentionPeriod) = launchLogged("retention") { updateSettings.retention(retention) }

    fun onThemeModeChange(mode: ThemeMode) = launchLogged("theme") { updateSettings.themeMode(mode) }

    fun onDynamicColorChange(enabled: Boolean) = launchLogged("dynamic color") { updateSettings.dynamicColor(enabled) }

    fun onKeepAliveChange(enabled: Boolean) = launchLogged("keep-alive") {
        updateSettings.keepAlive(enabled)
        if (enabled) keepAliveController.start() else keepAliveController.stop()
    }

    fun onSeedDemoData() = launchDemo("seed", R.string.settings_demo_added) {
        val result: DemoSeedResult = seedDemoData()
        AppLogger.i(TAG, "demo data added - {apis: ${result.apis}, sms: ${result.sms}, calls: ${result.calls}}")
    }

    fun onClearDemoData() = launchDemo("clear", R.string.settings_demo_removed) {
        val removed: Int = clearDemoData()
        AppLogger.i(TAG, "demo data removed - {apis: $removed}")
    }

    private fun launchDemo(action: String, @StringRes doneMessage: Int, block: suspend () -> Unit) {
        if (mutableDemoBusy.value) return
        mutableDemoBusy.value = true
        AppLogger.i(TAG, "demo data $action started")
        viewModelScope.launch {
            try {
                block()
                messageChannel.send(UiMessage(doneMessage))
            } catch (e: Exception) {
                AppLogger.e(TAG, "demo data $action failed", e)
                messageChannel.send(UiMessage(R.string.error_generic))
            } finally {
                mutableDemoBusy.value = false
            }
        }
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
