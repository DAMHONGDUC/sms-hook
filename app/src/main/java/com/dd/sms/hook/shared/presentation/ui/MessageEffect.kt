package com.dd.sms.hook.shared.presentation.ui

import android.content.res.Resources
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.flow.Flow

/** Shows each [UiMessage] from a ViewModel in the screen's snackbar. */
@Composable
fun MessageEffect(messages: Flow<UiMessage>, snackbarHostState: SnackbarHostState) {
    val resources: Resources = LocalResources.current

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(resources.getString(message.res, *message.args.toTypedArray()))
        }
    }
}
