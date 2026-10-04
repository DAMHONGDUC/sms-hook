package com.dd.sms.hook.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.ui.AppMessenger
import com.dd.sms.hook.shared.presentation.ui.LocalAppMessenger
import com.dd.sms.hook.features.apiconfig.presentation.editor.ApiEditorScreen
import com.dd.sms.hook.features.apiconfig.presentation.list.ApiListScreen
import com.dd.sms.hook.features.calllog.presentation.detail.CallDetailScreen
import com.dd.sms.hook.features.calllog.presentation.list.HistoryScreen
import com.dd.sms.hook.features.dashboard.presentation.DashboardScreen
import com.dd.sms.hook.features.settings.presentation.SettingsScreen
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    DASHBOARD(DashboardRoute, DashboardRoute::class, Icons.Filled.Insights, R.string.tab_dashboard),
    APIS(ApiListRoute, ApiListRoute::class, Icons.Filled.Api, R.string.tab_apis),
    HISTORY(HistoryRoute(), HistoryRoute::class, Icons.Filled.History, R.string.tab_history),
    SETTINGS(SettingsRoute, SettingsRoute::class, Icons.Filled.Settings, R.string.tab_settings),
}

@Composable
fun AppRoot(pendingCallLogId: MutableStateFlow<Long?>) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry: NavBackStackEntry? by navController.currentBackStackEntryAsState()
    val destination: NavDestination? = backStackEntry?.destination
    val showBottomBar: Boolean = TopLevelDestination.entries.any { destination?.hasRoute(it.routeClass) == true }
    val pendingLogId: Long? by pendingCallLogId.collectAsStateWithLifecycle()
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val scope: CoroutineScope = rememberCoroutineScope()
    val messenger: AppMessenger = remember(snackbarHostState, scope) {
        AppMessenger { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
    }

    LaunchedEffect(pendingLogId) {
        val id: Long = pendingLogId ?: return@LaunchedEffect
        navController.navigate(CallDetailRoute(id))
        pendingCallLogId.value = null
    }

    CompositionLocalProvider(LocalAppMessenger provides messenger) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination?.hasRoute(item.routeClass) == true,
                                onClick = { navController.navigateToTopLevel(item.route) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(text = stringResource(item.label), style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = DashboardRoute,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable<DashboardRoute> {
                    DashboardScreen(
                        onOpenCall = { navController.navigate(CallDetailRoute(it)) },
                        onOpenHistory = { navController.navigateToTopLevel(HistoryRoute()) },
                        onOpenSettings = { navController.navigateToTopLevel(SettingsRoute) },
                        onCreateApi = { navController.navigate(ApiEditorRoute()) },
                    )
                }
                composable<ApiListRoute> {
                    ApiListScreen(
                        onCreate = { navController.navigate(ApiEditorRoute()) },
                        onEdit = { navController.navigate(ApiEditorRoute(it)) },
                        onOpenHistory = { navController.navigate(HistoryRoute(it)) },
                    )
                }
                composable<HistoryRoute> {
                    HistoryScreen(
                        onOpenCall = { navController.navigate(CallDetailRoute(it)) },
                        onOpenApis = { navController.navigateToTopLevel(ApiListRoute) },
                        onNavigateUp = if (navController.previousBackStackEntry != null &&
                            it.toRoute<HistoryRoute>().configId != HistoryRoute.ALL_CONFIGS
                        ) {
                            { navController.navigateUp() }
                        } else {
                            null
                        },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen()
                }
                composable<ApiEditorRoute> {
                    ApiEditorScreen(onNavigateUp = { navController.navigateUp() })
                }
                composable<CallDetailRoute> {
                    CallDetailScreen(
                        onNavigateUp = { navController.navigateUp() },
                        onEditApi = { navController.navigate(ApiEditorRoute(it)) },
                    )
                }
            }
        }
    }
}

/** Tabs keep their own back stack and state instead of stacking on each other. */
private fun NavHostController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
