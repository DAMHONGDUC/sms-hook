package com.dd.sms.hook.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.SectionCard

/** Dev builds only: fills the app with demo data so every screen can be tried without real SMS. */
@Composable
internal fun DeveloperSection(viewModel: SettingsViewModel) {
    val busy: Boolean by viewModel.demoBusy.collectAsStateWithLifecycle()

    SectionCard(title = stringResource(R.string.settings_developer), icon = Icons.Filled.Code) {
        Text(
            text = stringResource(R.string.settings_demo_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
            FilledTonalButton(onClick = viewModel::onSeedDemoData, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.settings_demo_add), style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(onClick = viewModel::onClearDemoData, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.settings_demo_remove), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
