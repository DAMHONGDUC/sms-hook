package com.dd.sms.hook.features.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.permission.PermissionStatus
import com.dd.sms.hook.shared.presentation.permission.rememberPermissionRequester
import com.dd.sms.hook.shared.presentation.permission.rememberPermissionStatus
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.EmptyState
import com.dd.sms.hook.shared.presentation.ui.LoadingState
import com.dd.sms.hook.shared.presentation.ui.ScreenLevel
import com.dd.sms.hook.shared.presentation.ui.ScreenScaffold
import com.dd.sms.hook.shared.presentation.ui.SectionCard
import com.dd.sms.hook.shared.presentation.ui.UiFormat
import com.dd.sms.hook.features.calllog.presentation.components.CallLogRow
import com.dd.sms.hook.features.dashboard.domain.model.DashboardData
import com.dd.sms.hook.features.dashboard.domain.model.DashboardRange
import com.dd.sms.hook.features.dashboard.presentation.components.ApiBreakdownRow
import com.dd.sms.hook.features.dashboard.presentation.components.CallsBarChart
import com.dd.sms.hook.features.dashboard.presentation.components.GettingStartedCard
import com.dd.sms.hook.features.dashboard.presentation.components.SetupStep
import com.dd.sms.hook.features.dashboard.presentation.components.StatTile
import com.dd.sms.hook.features.dashboard.presentation.components.StatusHeroCard

private const val NO_VALUE = "—"

@Composable
fun DashboardScreen(
    onOpenCall: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateApi: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state: DashboardState by viewModel.state.collectAsStateWithLifecycle()
    val range: DashboardRange by viewModel.range.collectAsStateWithLifecycle()
    val permissions: PermissionStatus = rememberPermissionStatus()
    val requestPermissions: () -> Unit = rememberPermissionRequester(permissions)

    ScreenScaffold(title = stringResource(R.string.dashboard_title), level = ScreenLevel.TOP) { padding ->
        when (val current: DashboardState = state) {
            DashboardState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            DashboardState.Error -> EmptyState(
                icon = Icons.Filled.CloudOff,
                title = stringResource(R.string.dashboard_error_title),
                message = stringResource(R.string.error_generic),
                modifier = Modifier.padding(padding),
            )
            is DashboardState.Loaded -> Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.screenGutter),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap),
            ) {
                StatusHeroCard(current.data, onOpenSettings)
                val steps: List<SetupStep> = setupSteps(current.data, permissions, requestPermissions, onCreateApi)
                if (steps.any { !it.done }) GettingStartedCard(steps)
                RangeSelector(range, viewModel::onRangeChange)
                StatGrid(current.data)
                SectionCard(title = stringResource(R.string.dashboard_calls_per_day), icon = Icons.Filled.BarChart) {
                    CallsBarChart(days = current.data.daily)
                }
                TopApisCard(current.data)
                RecentCallsCard(current.data, onOpenCall, onOpenHistory)
            }
        }
    }
}

/** New users see three steps; returning users (all done) never see the card. */
private fun setupSteps(
    data: DashboardData,
    permissions: PermissionStatus,
    onRequestPermissions: () -> Unit,
    onCreateApi: () -> Unit,
): List<SetupStep> = listOf(
    SetupStep(
        title = R.string.getting_started_step_permissions,
        description = R.string.getting_started_step_permissions_desc,
        done = permissions.allGranted,
        actionLabel = R.string.setup_grant,
        onAction = onRequestPermissions,
    ),
    SetupStep(
        title = R.string.getting_started_step_api,
        description = R.string.getting_started_step_api_desc,
        done = data.enabledApis > 0,
        actionLabel = R.string.getting_started_add,
        onAction = onCreateApi,
    ),
    SetupStep(
        title = R.string.getting_started_step_sms,
        description = R.string.getting_started_step_sms_desc,
        done = data.hasForwardedSms,
        actionLabel = null,
        onAction = {},
    ),
)

@Composable
private fun RangeSelector(range: DashboardRange, onChange: (DashboardRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        DashboardRange.entries.forEachIndexed { index, item ->
            SegmentedButton(
                selected = item == range,
                onClick = { onChange(item) },
                shape = SegmentedButtonDefaults.itemShape(index, DashboardRange.entries.size),
            ) {
                Text(text = pluralStringResource(R.plurals.dashboard_range_days, item.days, item.days), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StatGrid(data: DashboardData) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.listItemGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.listItemGap)) {
            StatTile(stringResource(R.string.stat_sms_received), data.smsReceived.toString(), Icons.Filled.Sms, Modifier.weight(1f))
            StatTile(stringResource(R.string.stat_api_calls), data.summary.total.toString(), Icons.Filled.Api, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.listItemGap)) {
            StatTile(
                stringResource(R.string.stat_success_rate),
                if (data.summary.total == 0) NO_VALUE else UiFormat.percent(data.summary.successRate),
                Icons.Filled.TaskAlt,
                Modifier.weight(1f),
            )
            StatTile(
                stringResource(R.string.stat_avg_latency),
                if (data.summary.total == 0) NO_VALUE else UiFormat.duration(data.summary.avgDurationMs),
                Icons.Filled.Speed,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TopApisCard(data: DashboardData) {
    val maxTotal: Int = data.topApis.maxOfOrNull { it.total } ?: 0
    val deletedName: String = stringResource(R.string.dashboard_deleted_api)

    SectionCard(title = stringResource(R.string.dashboard_top_apis), icon = Icons.Filled.Leaderboard) {
        if (data.topApis.isEmpty()) NoCallsText()
        data.topApis.forEach { ApiBreakdownRow(item = it, maxTotal = maxTotal, fallbackName = deletedName) }
    }
}

@Composable
private fun RecentCallsCard(data: DashboardData, onOpenCall: (Long) -> Unit, onOpenHistory: () -> Unit) {
    SectionCard(title = stringResource(R.string.dashboard_recent), icon = Icons.Filled.History) {
        if (data.recent.isEmpty()) NoCallsText()
        data.recent.forEach { CallLogRow(log = it, onClick = { onOpenCall(it.id) }, horizontalPadding = 0.dp) }
        if (data.recent.isNotEmpty()) {
            TextButton(onClick = onOpenHistory) {
                Text(text = stringResource(R.string.dashboard_see_all), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = Dimens.smallGap))
            }
        }
    }
}

@Composable
private fun NoCallsText() {
    Text(
        text = stringResource(R.string.dashboard_no_calls),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
