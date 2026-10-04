package com.dd.sms.hook.features.apiconfig.presentation.list

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.CodeFontFamily
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.ConfirmDialog
import com.dd.sms.hook.shared.presentation.ui.EmptyState
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.shared.presentation.ui.LoadingState
import com.dd.sms.hook.shared.presentation.ui.MessageEffect
import com.dd.sms.hook.shared.presentation.ui.MethodTag
import com.dd.sms.hook.shared.presentation.ui.ScreenLevel
import com.dd.sms.hook.shared.presentation.ui.ScreenScaffold
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode

@Composable
fun ApiListScreen(
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenHistory: (Long) -> Unit,
    viewModel: ApiListViewModel = hiltViewModel(),
) {
    val state: ApiListState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val copySuffix: String = stringResource(R.string.api_list_copy_suffix)
    var pendingDelete: ApiConfig? by remember { mutableStateOf(null) }

    MessageEffect(viewModel.messages, snackbarHostState)

    ScreenScaffold(
        title = stringResource(R.string.api_list_title),
        level = ScreenLevel.TOP,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.api_list_new), style = MaterialTheme.typography.labelLarge) },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val current: ApiListState = state) {
                ApiListState.Loading -> LoadingState()
                is ApiListState.Loaded -> if (current.configs.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Api,
                        title = stringResource(R.string.api_list_empty_title),
                        message = stringResource(R.string.api_list_empty_message),
                        actionLabel = stringResource(R.string.api_list_new),
                        onAction = onCreate,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Dimens.screenGutter,
                            end = Dimens.screenGutter,
                            top = Dimens.listItemGap,
                            bottom = Dimens.fabClearance,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.listItemGap),
                    ) {
                        items(current.configs, key = { it.id }) { config ->
                            ApiConfigCard(
                                config = config,
                                onClick = { onEdit(config.id) },
                                onToggle = { viewModel.onToggle(config, it) },
                                onDuplicate = { viewModel.onDuplicate(config, copySuffix) },
                                onOpenHistory = { onOpenHistory(config.id) },
                                onDelete = { pendingDelete = config },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { config ->
        ConfirmDialog(
            title = stringResource(R.string.api_list_delete_title),
            message = stringResource(R.string.api_list_delete_message, config.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.onDelete(config) },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApiConfigCard(
    config: ApiConfig,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    onOpenHistory: () -> Unit,
    onDelete: () -> Unit,
) {
    val any: String = stringResource(R.string.api_filter_any)
    val regexSuffix: String = if (config.filter.mode == MatchMode.REGEX) " · " + stringResource(R.string.api_filter_regex_suffix) else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(start = Dimens.cardPadding, top = Dimens.cardPadding, bottom = Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.cardPadding)) {
                IconBadge(
                    icon = Icons.Filled.Api,
                    containerColor = if (config.enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (config.enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = config.enabled, onCheckedChange = onToggle)
                ApiCardMenu(onEdit = onClick, onDuplicate = onDuplicate, onOpenHistory = onOpenHistory, onDelete = onDelete)
            }
            Row(
                modifier = Modifier.padding(end = Dimens.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
            ) {
                MethodTag(config.method.name)
                Text(
                    text = config.url,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = CodeFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(
                modifier = Modifier.padding(end = Dimens.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.smallGap),
            ) {
                FilterTag(Icons.Filled.Person, stringResource(R.string.api_filter_from, config.filter.senders.ifBlank { any }))
                FilterTag(Icons.Filled.FilterAlt, stringResource(R.string.api_filter_contains, config.filter.keyword.ifBlank { any }) + regexSuffix)
            }
        }
    }
}

@Composable
private fun FilterTag(icon: ImageVector, label: String) {
    Surface(shape = RoundedCornerShape(Dimens.chipRadius), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.inlineGap, vertical = Dimens.smallGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSmall), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ApiCardMenu(onEdit: () -> Unit, onDuplicate: () -> Unit, onOpenHistory: () -> Unit, onDelete: () -> Unit) {
    var expanded: Boolean by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(R.string.action_edit, Icons.Filled.Edit) {
                expanded = false
                onEdit()
            }
            MenuItem(R.string.action_duplicate, Icons.Filled.ContentCopy) {
                expanded = false
                onDuplicate()
            }
            MenuItem(R.string.api_list_view_history, Icons.Filled.History) {
                expanded = false
                onOpenHistory()
            }
            MenuItem(R.string.action_delete, Icons.Filled.Delete) {
                expanded = false
                onDelete()
            }
        }
    }
}

@Composable
private fun MenuItem(@StringRes label: Int, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(label), style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}
