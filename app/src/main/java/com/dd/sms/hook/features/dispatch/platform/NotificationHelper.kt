package com.dd.sms.hook.features.dispatch.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.dd.sms.hook.MainActivity
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.constants.NotificationConstants
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.dispatch.domain.service.FailureNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationHelper"

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) : FailureNotifier {

    fun createChannels() {
        val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    NotificationConstants.CHANNEL_SERVICE,
                    context.getString(R.string.notification_channel_service),
                    NotificationManager.IMPORTANCE_MIN,
                ),
                NotificationChannel(
                    NotificationConstants.CHANNEL_FAILURES,
                    context.getString(R.string.notification_channel_failures),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    NotificationConstants.CHANNEL_WORK,
                    context.getString(R.string.notification_channel_work),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        )
    }

    fun keepAliveNotification(): Notification =
        NotificationCompat.Builder(context, NotificationConstants.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_forwarder)
            .setContentTitle(context.getString(R.string.notification_keep_alive_title))
            .setContentText(context.getString(R.string.notification_keep_alive_text))
            .setContentIntent(openAppIntent(logId = null))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    fun workForegroundInfo(): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_WORK)
            .setSmallIcon(R.drawable.ic_stat_forwarder)
            .setContentTitle(context.getString(R.string.notification_work_title))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(NotificationConstants.ID_WORK, notification)
    }

    override fun notifyFailure(log: CallLog) {
        if (!canPostNotifications()) {
            AppLogger.i(TAG, "failure notification skipped, permission missing - {log: ${log.id}}")
            return
        }
        val detail: String = log.responseCode?.let { "HTTP $it" } ?: log.errorMessage.orEmpty()
        val notification: Notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_FAILURES)
            .setSmallIcon(R.drawable.ic_stat_forwarder)
            .setContentTitle(context.getString(R.string.notification_failure_title, log.configName))
            .setContentText(context.getString(R.string.notification_failure_text, log.smsSender, detail))
            .setContentIntent(openAppIntent(log.id))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NotificationConstants.ID_FAILURE_BASE + (log.id % Int.MAX_VALUE).toInt(), notification)
            AppLogger.i(TAG, "failure notification posted - {log: ${log.id}, config: ${log.configName}}")
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "failure notification refused - {log: ${log.id}}", e)
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(logId: Long?): PendingIntent {
        val intent: Intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (logId != null) putExtra(MainActivity.EXTRA_CALL_LOG_ID, logId)
        }

        return PendingIntent.getActivity(
            context,
            logId?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
