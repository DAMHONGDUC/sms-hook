package com.dd.sms.hook.features.calllog

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import com.dd.sms.hook.features.apiconfig.domain.usecase.ObserveApiConfigsUseCase
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.usecase.ClearCallLogsUseCase
import com.dd.sms.hook.features.calllog.domain.usecase.ObserveCallLogsUseCase
import com.dd.sms.hook.features.calllog.presentation.list.HistoryState
import com.dd.sms.hook.features.calllog.presentation.list.HistoryViewModel
import com.dd.sms.hook.features.dispatch.domain.usecase.ObserveCallQueueUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.ObserveRetryableFailuresUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.RetryFailedCallsUseCase
import com.dd.sms.hook.navigation.HistoryRoute
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeCallScheduler
import com.dd.sms.hook.testing.FakeReceivedSmsRepository
import com.dd.sms.hook.testing.Fixtures
import com.dd.sms.hook.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

private const val PAST_DEBOUNCE_MILLIS = 300L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now: Long = TimeUtils.now()
    private val logs = FakeCallLogRepository(
        listOf(
            Fixtures.log(id = 1L, configId = 1L, createdAt = now, status = CallStatus.SUCCESS),
            Fixtures.log(id = 2L, configId = 2L, createdAt = now - 1000, status = CallStatus.FAILED, configName = "Backup"),
            Fixtures.log(id = 3L, configId = 1L, createdAt = now - 2 * TimeUtils.MILLIS_PER_DAY),
        )
    )

    private val configs = FakeApiConfigRepository(listOf(Fixtures.config(id = 1L), Fixtures.config(id = 2L).copy(name = "Backup")))
    private val scheduler = FakeCallScheduler()

    private fun viewModel(configId: Long = HistoryRoute.ALL_CONFIGS): HistoryViewModel {
        val observeFailures = ObserveRetryableFailuresUseCase(logs, scheduler)

        return HistoryViewModel(
            SavedStateHandle(route = HistoryRoute(configId)),
            ObserveCallLogsUseCase(logs),
            ObserveApiConfigsUseCase(configs),
            ObserveCallQueueUseCase(scheduler, configs, FakeReceivedSmsRepository()),
            observeFailures,
            ClearCallLogsUseCase(logs),
            RetryFailedCallsUseCase(observeFailures, configs, scheduler),
        )
    }

    private fun TestScope.loaded(vm: HistoryViewModel): HistoryState.Loaded {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        advanceTimeBy(PAST_DEBOUNCE_MILLIS)
        return vm.state.value as HistoryState.Loaded
    }

    @Test
    fun `logs are grouped by local day, newest day first`() = runTest {
        val state: HistoryState.Loaded = loaded(viewModel())

        assertEquals(listOf(LocalDate.now(), LocalDate.now().minusDays(2)), state.days.map { it.day })
        assertEquals(listOf(1L, 2L), state.days.first().logs.map { it.id })
    }

    @Test
    fun `status and api filters narrow the list`() = runTest {
        val vm: HistoryViewModel = viewModel()

        vm.onStatusFilter(CallStatus.FAILED)
        assertEquals(listOf(2L), loaded(vm).days.flatMap { it.logs }.map { it.id })
        vm.onStatusFilter(null)
        vm.onApiFilter(1L)
        advanceTimeBy(PAST_DEBOUNCE_MILLIS)
        assertEquals(listOf(1L, 3L), (vm.state.value as HistoryState.Loaded).days.flatMap { it.logs }.map { it.id })
    }

    @Test
    fun `route config id preselects the api filter and clear resets everything`() = runTest {
        val vm: HistoryViewModel = viewModel(configId = 2L)

        assertTrue(vm.isScopedToApi)
        assertEquals(listOf(2L), loaded(vm).days.flatMap { it.logs }.map { it.id })
        vm.onQueryChange("zzz")
        vm.onClearFilters()
        assertEquals(CallLogFilter.ALL, vm.filter.value)
    }

    @Test
    fun `clear all empties the history`() = runTest {
        val vm: HistoryViewModel = viewModel()

        vm.onClearAll()

        assertTrue(loaded(vm).isEmpty)
    }

    @Test
    fun `queue follows the selected api`() = runTest {
        scheduler.queue.value = listOf(Fixtures.queued(configId = 1L), Fixtures.queued(configId = 2L))
        val vm: HistoryViewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.queue.collect { } }

        assertEquals(2, vm.queue.value.size)
        vm.onApiFilter(2L)
        assertEquals(listOf("Backup"), vm.queue.value.map { it.configName })
    }

    @Test
    fun `retry all is offered only for one api and queues its failures`() = runTest {
        val vm: HistoryViewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.retryableFailures.collect { } }

        assertEquals(0, vm.retryableFailures.value)
        vm.onApiFilter(2L)
        assertEquals(1, vm.retryableFailures.value)
        vm.onRetryAllFailed()
        assertEquals(listOf(Triple(2L, 3L, CallTrigger.RETRY)), scheduler.enqueued)
    }
}
