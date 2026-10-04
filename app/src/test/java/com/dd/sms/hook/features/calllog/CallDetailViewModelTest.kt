package com.dd.sms.hook.features.calllog

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.dd.sms.hook.R
import com.dd.sms.hook.navigation.CallDetailRoute
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.usecase.DeleteCallLogUseCase
import com.dd.sms.hook.features.calllog.domain.usecase.ObserveCallAttemptsUseCase
import com.dd.sms.hook.features.calllog.domain.usecase.ObserveCallLogUseCase
import com.dd.sms.hook.features.calllog.presentation.detail.CallDetailEvent
import com.dd.sms.hook.features.calllog.presentation.detail.CallDetailState
import com.dd.sms.hook.features.calllog.presentation.detail.CallDetailViewModel
import com.dd.sms.hook.features.dispatch.domain.usecase.RetryCallUseCase
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeCallScheduler
import com.dd.sms.hook.testing.Fixtures
import com.dd.sms.hook.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CallDetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val logs = FakeCallLogRepository(
        listOf(
            Fixtures.log(id = 1L, attempt = 1, status = CallStatus.FAILED, createdAt = 100L),
            Fixtures.log(id = 2L, attempt = 2, status = CallStatus.SUCCESS, createdAt = 200L),
            Fixtures.log(id = 3L, configId = null, smsId = null, trigger = CallTrigger.TEST),
        )
    )
    private val scheduler = FakeCallScheduler()

    private fun viewModel(id: Long): CallDetailViewModel = CallDetailViewModel(
        SavedStateHandle(route = CallDetailRoute(id)),
        ObserveCallLogUseCase(logs),
        ObserveCallAttemptsUseCase(logs),
        RetryCallUseCase(logs, FakeApiConfigRepository(listOf(Fixtures.config(id = 7L))), scheduler),
        DeleteCallLogUseCase(logs),
    )

    private fun TestScope.state(vm: CallDetailViewModel): CallDetailState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        return vm.state.value
    }

    @Test
    fun `sms call loads with every attempt oldest first`() = runTest {
        val state = state(viewModel(2L)) as CallDetailState.Loaded

        assertEquals(2L, state.log.id)
        assertEquals(listOf(1L, 2L), state.attempts.map { it.id })
    }

    @Test
    fun `test call has no timeline and unknown id is not found`() = runTest {
        assertTrue((state(viewModel(3L)) as CallDetailState.Loaded).attempts.isEmpty())
        assertEquals(CallDetailState.NotFound, state(viewModel(99L)))
    }

    @Test
    fun `retry queues the call and reports it`() = runTest {
        val vm: CallDetailViewModel = viewModel(1L)

        vm.events.test {
            vm.onRetry()
            assertEquals(R.string.detail_retry_queued, (awaitItem() as CallDetailEvent.Message).message.res)
        }
        assertEquals(CallTrigger.RETRY, scheduler.enqueued.single().third)
    }

    @Test
    fun `delete removes the log and closes the screen`() = runTest {
        val vm: CallDetailViewModel = viewModel(1L)

        vm.events.test {
            vm.onDelete()
            assertEquals(CallDetailEvent.Deleted, awaitItem())
        }
        assertEquals(listOf(2L, 3L), logs.logs.value.map { it.id })
    }
}
