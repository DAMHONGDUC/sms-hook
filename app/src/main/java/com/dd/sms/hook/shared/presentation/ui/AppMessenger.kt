package com.dd.sms.hook.shared.presentation.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Shows a snackbar on the app's root scaffold, so feedback survives the screen that raised it closing
 * (e.g. "saved" after the editor pops).
 */
fun interface AppMessenger {
    fun show(message: String)
}

val LocalAppMessenger = staticCompositionLocalOf<AppMessenger> { AppMessenger { } }
