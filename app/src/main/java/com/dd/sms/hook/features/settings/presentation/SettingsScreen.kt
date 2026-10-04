package com.dd.sms.hook.features.settings.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ForwardToInbox
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.BuildConfig
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.permission.rememberPermissionStatus
import com.dd.sms.hook.shared.presentation.theme.CodeFontFamily
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.KeyValueRow
import com.dd.sms.hook.shared.presentation.ui.LoadingState
import com.dd.sms.hook.shared.presentation.ui.PermissionSetupCard
import com.dd.sms.hook.shared.presentation.ui.ScreenLevel
import com.dd.sms.hook.shared.presentation.ui.ScreenScaffold
import com.dd.sms.hook.shared.presentation.ui.SectionCard
import com.dd.sms.hook.shared.presentation.ui.SwitchRow
import com.dd.sms.hook.features.dispatch.domain.service.TemplateVariables
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings: AppSettings? by viewModel.settings.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.settings_title), level = ScreenLevel.TOP) { padding ->
        val current: AppSettings = settings ?: run {
            LoadingState(modifier = Modifier.padding(padding))
            return@ScreenScaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap),
        ) {
            SectionCard(title = stringResource(R.string.settings_section_forwarding), icon = Icons.AutoMirrored.Filled.ForwardToInbox) {
                SettingSwitchRow(R.string.settings_forwarding, R.string.settings_forwarding_desc, current.forwardingEnabled, viewModel::onForwardingChange)
                SettingSwitchRow(R.string.settings_keep_alive, R.string.settings_keep_alive_desc, current.keepAliveEnabled, viewModel::onKeepAliveChange)
                SettingSwitchRow(R.string.settings_notify, R.string.settings_notify_desc, current.notifyOnFailure, viewModel::onNotifyOnFailureChange)
            }
            AppearanceSection(current, viewModel)
            PermissionSetupCard(rememberPermissionStatus())
            SectionCard(title = stringResource(R.string.settings_retention), icon = Icons.Filled.AutoDelete) {
                Text(
                    text = stringResource(R.string.settings_retention_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RetentionPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = current.retention == period,
                            onClick = { viewModel.onRetentionChange(period) },
                            shape = SegmentedButtonDefaults.itemShape(index, RetentionPeriod.entries.size),
                            icon = {},
                        ) {
                            Text(text = retentionLabel(period), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            PlaceholderHelpCard()
            SectionCard(title = stringResource(R.string.settings_about), icon = Icons.Filled.Info) {
                KeyValueRow(stringResource(R.string.settings_version), versionLabel())
            }
        }
    }
}

@Composable
internal fun SettingSwitchRow(@StringRes title: Int, @StringRes description: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    SwitchRow(title = stringResource(title), description = stringResource(description), checked = checked, onChange = onChange)
}

@Composable
private fun PlaceholderHelpCard() {
    SectionCard(title = stringResource(R.string.settings_placeholders), icon = Icons.Filled.DataObject) {
        TemplateVariables.ALL.forEach { name ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
                Text(
                    text = TemplateVariables.token(name),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = CodeFontFamily),
                    modifier = Modifier.weight(0.45f),
                )
                Text(
                    text = stringResource(placeholderDescription(name)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.55f),
                )
            }
        }
    }
}

/** Version plus the build environment from env/env.properties, e.g. "2.0.0 · prod". */
private fun versionLabel(): String =
    listOf(BuildConfig.VERSION_NAME, BuildConfig.ENV).filter { it.isNotBlank() }.joinToString(" · ")

@StringRes
private fun placeholderDescription(name: String): Int = when (name) {
    TemplateVariables.SENDER -> R.string.placeholder_sender
    TemplateVariables.BODY -> R.string.placeholder_body
    TemplateVariables.RECEIVED_AT -> R.string.placeholder_received_at
    TemplateVariables.RECEIVED_AT_ISO -> R.string.placeholder_received_at_iso
    TemplateVariables.SIM -> R.string.placeholder_sim
    else -> R.string.placeholder_config_name
}

@Composable
private fun retentionLabel(period: RetentionPeriod): String =
    period.days?.let { pluralStringResource(R.plurals.settings_retention_days, it, it) } ?: stringResource(R.string.settings_retention_forever)
