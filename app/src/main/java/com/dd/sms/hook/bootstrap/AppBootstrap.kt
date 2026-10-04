package com.dd.sms.hook.bootstrap

import com.dd.sms.hook.shared.data.di.ApplicationScope
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.dispatch.data.work.WorkManagerCallScheduler
import com.dd.sms.hook.features.dispatch.domain.service.KeepAliveController
import com.dd.sms.hook.features.dispatch.platform.NotificationHelper
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AppBootstrap"

/** Startup work. Each step is guarded on its own so one failure cannot skip the others. */
class AppBootstrap @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val scheduler: WorkManagerCallScheduler,
    private val settingsRepository: SettingsRepository,
    private val keepAliveController: KeepAliveController,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    fun run() {
        try {
            notificationHelper.createChannels()
        } catch (e: Exception) {
            AppLogger.e(TAG, "creating notification channels failed", e)
        }
        try {
            scheduler.schedulePeriodicCleanup()
        } catch (e: Exception) {
            AppLogger.e(TAG, "scheduling cleanup failed", e)
        }
        appScope.launch {
            try {
                if (settingsRepository.current().keepAliveEnabled) keepAliveController.start()
            } catch (e: Exception) {
                AppLogger.e(TAG, "starting keep-alive failed", e)
            }
        }
    }
}
