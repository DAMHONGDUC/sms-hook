package com.dd.sms.hook.features.apiconfig.presentation.editor

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.AppMessenger
import com.dd.sms.hook.shared.presentation.ui.LoadingState
import com.dd.sms.hook.shared.presentation.ui.LocalAppMessenger
import com.dd.sms.hook.shared.presentation.ui.ScreenLevel
import com.dd.sms.hook.shared.presentation.ui.ScreenScaffold

private val BOTTOM_BAR_ELEVATION = 3.dp
private val PROGRESS_STROKE = 2.dp

@Composable
fun ApiEditorScreen(
    onNavigateUp: () -> Unit,
    viewModel: ApiEditorViewModel = hiltViewModel(),
) {
    val state: ApiEditorState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val resources: Resources = LocalResources.current
    val messenger: AppMessenger = LocalAppMessenger.current
    var showTestDialog: Boolean by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Closure for the user: the confirmation stays visible on the screen they return to.
                is ApiEditorEvent.Saved -> {
                    messenger.show(resources.getString(R.string.editor_saved, event.name))
                    onNavigateUp()
                }
                is ApiEditorEvent.Message -> snackbarHostState.showSnackbar(
                    resources.getString(event.message.res, *event.message.args.toTypedArray())
                )
            }
        }
    }

    ScreenScaffold(
        title = stringResource(if (viewModel.isNew) R.string.editor_title_new else R.string.editor_title_edit),
        level = ScreenLevel.DETAIL,
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            if (!state.loading) {
                EditorActionBar(
                    saving = state.saving,
                    onTest = { showTestDialog = true },
                    onSave = viewModel::onSave,
                )
            }
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.screenGutter),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap),
            ) {
                GeneralSection(state, viewModel)
                RequestSection(state, viewModel)
                TriggerSection(state, viewModel)
                DeliverySection(state, viewModel)
            }
        }
    }

    if (showTestDialog) {
        TestCallDialog(
            testCall = state.testCall,
            onRun = viewModel::onRunTest,
            onDismiss = {
                showTestDialog = false
                viewModel.onDismissTest()
            },
        )
    }
}

/** Primary actions sit in the thumb zone and ride above the keyboard. */
@Composable
private fun EditorActionBar(saving: Boolean, onTest: () -> Unit, onSave: () -> Unit) {
    Surface(tonalElevation = BOTTOM_BAR_ELEVATION, shadowElevation = BOTTOM_BAR_ELEVATION) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.listItemGap),
            horizontalArrangement = Arrangement.spacedBy(Dimens.listItemGap),
        ) {
            OutlinedButton(onClick = onTest, modifier = Modifier.weight(1f), contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(
                    text = stringResource(R.string.editor_test_button),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = PROGRESS_STROKE)
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                }
                Text(
                    text = stringResource(R.string.action_save),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
        }
    }
}
