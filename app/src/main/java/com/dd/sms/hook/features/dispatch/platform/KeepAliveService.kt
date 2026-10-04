package com.dd.sms.hook.features.dispatch.platform

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.dd.sms.hook.shared.domain.constants.NotificationConstants
import com.dd.sms.hook.shared.domain.logging.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "KeepAliveService"

/** A do-nothing foreground service: its only job is to keep the process ranked high so SMS handling is instant. */
@AndroidEntryPoint
class KeepAliveService : Service() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NotificationConstants.ID_KEEP_ALIVE,
                notificationHelper.keepAliveNotification(),
                type,
            )
            AppLogger.i(TAG, "foreground started - {startId: $startId, flags: $flags}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "foreground start refused - stopping {startId: $startId}", e)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        AppLogger.i(TAG, "destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
