package com.dd.sms.hook.shared.presentation.ui

import androidx.annotation.StringRes

/** A one-shot, localized message from a ViewModel to its screen's snackbar. */
data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())
