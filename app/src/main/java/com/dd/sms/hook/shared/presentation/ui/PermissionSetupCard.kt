package com.dd.sms.hook.shared.presentation.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.shared.presentation.permission.PermissionStatus
import com.dd.sms.hook.shared.presentation.permission.PermissionUtils
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.Dimens

private const val TAG = "PermissionSetupCard"

/** Lists what the forwarder still needs from the system, with one action per missing item. */
@Composable
fun PermissionSetupCard(status: PermissionStatus, modifier: Modifier = Modifier) {
    val context: Context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        AppLogger.i(TAG, "permission result - $result")
        if (result.values.any { !it }) PermissionUtils.openAppSettings(context)
    }

    SectionCard(modifier = modifier, title = stringResource(R.string.setup_title), icon = Icons.Filled.Checklist) {
        Text(
            text = stringResource(R.string.setup_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SetupRow(Icons.Filled.Sms, R.string.setup_sms, status.sms) { launcher.launch(PermissionUtils.runtimePermissions) }
        SetupRow(Icons.Filled.Notifications, R.string.setup_notifications, status.notifications) {
            launcher.launch(PermissionUtils.runtimePermissions)
        }
        SetupRow(Icons.Filled.BatteryAlert, R.string.setup_battery, status.batteryUnrestricted) {
            PermissionUtils.requestBatteryExemption(context)
        }
    }
}

@Composable
private fun SetupRow(icon: ImageVector, label: Int, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconMedium))
        Text(text = stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (granted) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.setup_granted),
                tint = AppThemeExtras.statusColors.success,
            )
        } else {
            TextButton(onClick = onGrant) {
                Text(text = stringResource(R.string.setup_grant), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
