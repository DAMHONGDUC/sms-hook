package com.dd.sms.hook.features.dispatch.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dd.sms.hook.shared.data.di.ApplicationScope
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.dispatch.domain.service.KeepAliveController
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootReceiver"

/** Restarts the keep-alive service after reboot or app update when the user enabled it. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var keepAliveController: KeepAliveController

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending: PendingResult = goAsync()

        AppLogger.i(TAG, "received - {action: ${intent.action}}")
        appScope.launch {
            try {
                if (settingsRepository.current().keepAliveEnabled) keepAliveController.start()
            } catch (e: Exception) {
                AppLogger.e(TAG, "restart after boot failed - {action: ${intent.action}}", e)
            } finally {
                pending.finish()
            }
        }
    }
}
