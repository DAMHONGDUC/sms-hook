package com.dd.sms.hook.features.dispatch.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.dispatch.domain.service.KeepAliveController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "KeepAliveController"

class AndroidKeepAliveController @Inject constructor(
    @ApplicationContext private val context: Context,
) : KeepAliveController {
    override fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
            AppLogger.i(TAG, "start requested")
        } catch (e: Exception) {
            // Android 12+ refuses FGS starts from the background; the next app open or boot retries.
            AppLogger.e(TAG, "start refused", e)
        }
    }

    override fun stop() {
        val stopped: Boolean = context.stopService(Intent(context, KeepAliveService::class.java))
        AppLogger.i(TAG, "stop requested - {wasRunning: $stopped}")
    }
}
