package com.dd.sms.hook.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.dd.sms.hook.shared.domain.constants.WorkConstants
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.data.work.ApiCallWorker
import com.dd.sms.hook.features.dispatch.domain.model.DispatchOutcome
import com.dd.sms.hook.features.dispatch.domain.usecase.ExecuteQueuedCallUseCase
import com.dd.sms.hook.features.dispatch.platform.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Maps use-case outcomes to WorkManager results and passes the input data and attempt number through. */
@RunWith(AndroidJUnit4::class)
class ApiCallWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val useCase: ExecuteQueuedCallUseCase = mockk()
    private val log: CallLog = mockk(relaxed = true)

    private fun worker(runAttemptCount: Int = 0, trigger: CallTrigger = CallTrigger.SMS): ApiCallWorker =
        TestListenableWorkerBuilder<ApiCallWorker>(
            context,
            workDataOf(
                WorkConstants.KEY_CONFIG_ID to 7L,
                WorkConstants.KEY_SMS_ID to 3L,
                WorkConstants.KEY_TRIGGER to trigger.name,
            ),
        )
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                    ApiCallWorker(appContext, workerParameters, useCase, NotificationHelper(appContext))
            })
            .build()

    @Test
    fun successAndSkipAreSuccess() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns DispatchOutcome.Success(log)
        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        coEvery { useCase(any(), any(), any(), any()) } returns DispatchOutcome.Skipped("gone")
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
    }

    @Test
    fun failureRetriesOnlyWhenTheUseCaseSaysSo() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns DispatchOutcome.Failed(log, willRetry = true)
        assertEquals(ListenableWorker.Result.retry(), worker().doWork())

        coEvery { useCase(any(), any(), any(), any()) } returns DispatchOutcome.Failed(log, willRetry = false)
        assertEquals(ListenableWorker.Result.failure(), worker().doWork())
    }

    @Test
    fun inputDataAndAttemptArePassedThrough() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns DispatchOutcome.Success(log)

        worker(runAttemptCount = 2, trigger = CallTrigger.RETRY).doWork()

        coVerify { useCase(7L, 3L, 3, CallTrigger.RETRY) }
    }

    @Test
    fun crashInUseCaseIsAFailureNotACrash() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } throws IllegalStateException("boom")

        assertEquals(ListenableWorker.Result.failure(), worker().doWork())
    }
}
