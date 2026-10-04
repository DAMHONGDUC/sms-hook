package com.dd.sms.hook.shared.presentation.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dd.sms.hook.shared.domain.logging.AppLogger

private const val TAG = "PermissionUtils"

/** Everything the app needs from the system to forward SMS reliably. */
data class PermissionStatus(
    val sms: Boolean,
    val notifications: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val allGranted: Boolean get() = sms && notifications && batteryUnrestricted
}

object PermissionUtils {
    /** Runtime permissions requested together from the setup card. */
    val runtimePermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECEIVE_SMS)
        }

    fun status(context: Context): PermissionStatus = PermissionStatus(
        sms = isGranted(context, Manifest.permission.RECEIVE_SMS),
        notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
    )

    /** The direct exemption prompt; this app's purpose is exactly the case the policy allows. */
    @SuppressLint("BatteryLife")
    fun requestBatteryExemption(context: Context) {
        val direct: Intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData("package:${context.packageName}".toUri())

        try {
            context.startActivity(direct)
            AppLogger.i(TAG, "battery exemption prompt opened")
        } catch (e: Exception) {
            AppLogger.e(TAG, "battery exemption prompt unavailable - opening list", e)
            openSafely(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    fun openAppSettings(context: Context) {
        openSafely(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData("package:${context.packageName}".toUri()),
        )
    }

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun openSafely(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
            AppLogger.i(TAG, "settings opened - {action: ${intent.action}}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "settings screen unavailable - {action: ${intent.action}}", e)
        }
    }
}
