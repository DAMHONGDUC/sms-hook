package com.dd.sms.hook.features.apiconfig.presentation.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.CodeBlock
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.shared.presentation.ui.UiFormat
import com.dd.sms.hook.shared.presentation.ui.rememberCopyAction
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallStatus

private const val BADGE_START_SCALE = 0.6f

@Composable
internal fun TestCallDialog(
    testCall: TestCallState,
    onRun: (sender: String, body: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultSender: String = stringResource(R.string.test_default_sender)
    val defaultBody: String = stringResource(R.string.test_default_body)
    var sender: String by rememberSaveable { mutableStateOf(defaultSender) }
    var body: String by rememberSaveable { mutableStateOf(defaultBody) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.test_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
            ) {
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text(text = stringResource(R.string.test_sender), style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(text = stringResource(R.string.test_body), style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                )
                when (testCall) {
                    TestCallState.Idle -> Unit
                    TestCallState.Running -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is TestCallState.Done -> TestResult(testCall.log)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRun(sender, body) }, enabled = testCall != TestCallState.Running) {
                Text(text = stringResource(R.string.test_send), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun TestResult(log: CallLog) {
    val success: Boolean = log.status == CallStatus.SUCCESS
    val statusColor: Color = if (success) AppThemeExtras.statusColors.success else AppThemeExtras.statusColors.failure
    val copy: (String) -> Unit = rememberCopyAction()
    val badgeScale = remember(log.id) { Animatable(BADGE_START_SCALE) }
    val code: Int? = log.responseCode
    val message: String = when {
        success && code != null -> stringResource(R.string.test_success_message, code, UiFormat.duration(log.durationMs))
        code != null -> stringResource(R.string.test_failure_message_code, code)
        else -> stringResource(R.string.test_failure_message_error)
    }

    // The peak of setting up an API: a short, gentle settle-in, never a loop.
    LaunchedEffect(log.id) {
        badgeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.rowGap)) {
        IconBadge(
            icon = if (success) Icons.Filled.Check else Icons.Filled.PriorityHigh,
            size = Dimens.badgeLarge,
            containerColor = statusColor,
            contentColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.scale(badgeScale.value),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
            Text(
                text = stringResource(if (success) R.string.test_success_title else R.string.test_failure_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            if (!success) {
                Text(
                    text = stringResource(R.string.test_failure_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    CodeBlock(label = stringResource(R.string.detail_request_body), text = log.requestBody, onCopy = copy)
    CodeBlock(
        label = stringResource(R.string.detail_response_body),
        text = log.responseBody ?: log.errorMessage.orEmpty(),
        onCopy = copy,
    )
}
