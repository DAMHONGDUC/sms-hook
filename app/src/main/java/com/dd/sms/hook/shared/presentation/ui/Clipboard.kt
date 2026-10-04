package com.dd.sms.hook.shared.presentation.ui

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val CLIP_LABEL = "sms-forwarder"

/** Returns a copy action bound to the current clipboard. */
@Composable
fun rememberCopyAction(): (String) -> Unit {
    val clipboard: Clipboard = LocalClipboard.current
    val scope: CoroutineScope = rememberCoroutineScope()

    return { text: String ->
        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(CLIP_LABEL, text))) }
    }
}
