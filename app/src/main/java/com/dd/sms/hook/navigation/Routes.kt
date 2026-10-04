package com.dd.sms.hook.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data object ApiListRoute

/** [configId] of [ALL_CONFIGS] shows history for every API. */
@Serializable
data class HistoryRoute(val configId: Long = ALL_CONFIGS) {
    companion object {
        const val ALL_CONFIGS: Long = 0L
    }
}

@Serializable
data object SettingsRoute

/** [id] of [NEW] opens an empty editor. */
@Serializable
data class ApiEditorRoute(val id: Long = NEW) {
    companion object {
        const val NEW: Long = 0L
    }
}

@Serializable
data class CallDetailRoute(val id: Long)
