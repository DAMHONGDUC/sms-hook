package com.dd.sms.hook.testing

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.dd.sms.hook.shared.domain.constants.WorkConstants
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.components.SingletonComponent
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.UUID

/**
 * Injects the test and starts a test WorkManager with Hilt workers, before the activity rule launches.
 * Order: HiltAndroidRule (0) -> this (1) -> compose/activity rule (2).
 */
class WorkManagerTestRule(private val hiltRule: HiltAndroidRule) : TestWatcher() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override fun starting(description: Description) {
        hiltRule.inject()
        val context: Context = ApplicationProvider.getApplicationContext()
        val workerFactory: HiltWorkerFactory =
            EntryPointAccessors.fromApplication(context, WorkerFactoryEntryPoint::class.java).workerFactory()
        val config: Configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(workerFactory)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    /** Marks every queued API call's constraints (network) as met so it runs now. */
    fun runQueuedCalls() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val driver = WorkManagerTestInitHelper.getTestDriver(context) ?: error("test WorkManager not initialised")
        val infos: List<WorkInfo> = WorkManager.getInstance(context).getWorkInfosByTag(WorkConstants.TAG_API_CALL).get()

        infos.filter { it.state == WorkInfo.State.ENQUEUED }.forEach { info: WorkInfo ->
            val id: UUID = info.id
            driver.setAllConstraintsMet(id)
            driver.setInitialDelayMet(id)
        }
    }

    fun callStates(): List<WorkInfo.State> {
        val context: Context = ApplicationProvider.getApplicationContext()

        return WorkManager.getInstance(context).getWorkInfosByTag(WorkConstants.TAG_API_CALL).get().map { it.state }
    }
}
