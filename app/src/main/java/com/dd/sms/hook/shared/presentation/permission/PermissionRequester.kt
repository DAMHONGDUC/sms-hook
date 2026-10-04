package com.dd.sms.hook.shared.presentation.permission

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dd.sms.hook.shared.domain.logging.AppLogger

private const val TAG = "PermissionRequester"

/**
 * One action that asks for whatever is still missing: runtime permissions first, then the battery exemption.
 * A denied runtime permission opens app settings, since Android stops showing the prompt after repeated denials.
 */
@Composable
fun rememberPermissionRequester(status: PermissionStatus): () -> Unit {
    val context: Context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        AppLogger.i(TAG, "permission result - $result")
        if (result.values.any { !it }) PermissionUtils.openAppSettings(context)
    }

    return {
        AppLogger.i(TAG, "request tapped - $status")
        if (!status.sms || !status.notifications) {
            launcher.launch(PermissionUtils.runtimePermissions)
        } else if (!status.batteryUnrestricted) {
            PermissionUtils.requestBatteryExemption(context)
        }
    }
}
