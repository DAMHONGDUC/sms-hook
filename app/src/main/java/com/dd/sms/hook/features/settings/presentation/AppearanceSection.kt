package com.dd.sms.hook.features.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.locale.AppLanguage
import com.dd.sms.hook.shared.presentation.locale.LocaleController
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.theme.ThemeSupport
import com.dd.sms.hook.shared.presentation.ui.SectionCard
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.ThemeMode

/** Theme, dynamic colour and language. Language is stored by the system, not in app settings. */
@Composable
internal fun AppearanceSection(settings: AppSettings, viewModel: SettingsViewModel) {
    var showLanguages: Boolean by rememberSaveable { mutableStateOf(false) }
    val language: AppLanguage = LocaleController.current()

    SectionCard(title = stringResource(R.string.settings_section_appearance), icon = Icons.Filled.Palette) {
        Text(text = stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.onThemeModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    icon = { Icon(themeIcon(mode), contentDescription = null, modifier = Modifier.padding(end = Dimens.smallGap)) },
                ) {
                    Text(text = stringResource(themeLabel(mode)), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (ThemeSupport.dynamicColorAvailable) {
            SettingSwitchRow(
                title = R.string.settings_dynamic_color,
                description = R.string.settings_dynamic_color_desc,
                checked = settings.dynamicColor,
                onChange = viewModel::onDynamicColorChange,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLanguages = true }
                .padding(vertical = Dimens.inlineGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.cardPadding),
        ) {
            Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = languageName(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showLanguages) {
        LanguageDialog(
            selected = language,
            onSelect = {
                showLanguages = false
                LocaleController.apply(it)
            },
            onDismiss = { showLanguages = false },
        )
    }
}

@Composable
private fun LanguageDialog(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_language), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = language == selected, role = Role.RadioButton, onClick = { onSelect(language) })
                            .padding(vertical = Dimens.smallGap),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
                    ) {
                        RadioButton(selected = language == selected, onClick = null)
                        Text(text = languageName(language), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun languageName(language: AppLanguage): String =
    language.nativeName ?: stringResource(R.string.settings_language_system)

private fun themeIcon(mode: ThemeMode): ImageVector = when (mode) {
    ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
