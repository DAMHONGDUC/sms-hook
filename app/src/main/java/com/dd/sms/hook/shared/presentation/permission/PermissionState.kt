package com.dd.sms.hook.shared.presentation.permission

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Permission status re-read every time the screen resumes, e.g. after returning from system settings. */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context: Context = LocalContext.current
    var status: PermissionStatus by remember { mutableStateOf(PermissionUtils.status(context)) }

    LifecycleResumeEffect(context) {
        status = PermissionUtils.status(context)
        onPauseOrDispose { }
    }
    return status
}
