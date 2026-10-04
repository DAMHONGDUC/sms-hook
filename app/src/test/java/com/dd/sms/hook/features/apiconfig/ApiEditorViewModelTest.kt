package com.dd.sms.hook.features.apiconfig

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.dd.sms.hook.R
import com.dd.sms.hook.navigation.ApiEditorRoute
import com.dd.sms.hook.features.apiconfig.domain.model.HttpMethod
import com.dd.sms.hook.features.apiconfig.domain.service.ApiConfigError
import com.dd.sms.hook.features.apiconfig.domain.service.ApiConfigValidator
import com.dd.sms.hook.features.apiconfig.domain.usecase.GetApiConfigUseCase
import com.dd.sms.hook.features.apiconfig.domain.usecase.SaveApiConfigUseCase
import com.dd.sms.hook.features.apiconfig.presentation.editor.ApiEditorEvent
import com.dd.sms.hook.features.apiconfig.presentation.editor.ApiEditorViewModel
import com.dd.sms.hook.features.apiconfig.presentation.editor.TestCallState
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.dispatch.domain.model.HttpResult
import com.dd.sms.hook.features.dispatch.domain.service.RequestFactory
import com.dd.sms.hook.features.dispatch.domain.service.TemplateRenderer
import com.dd.sms.hook.features.dispatch.domain.usecase.CallExecution
import com.dd.sms.hook.features.dispatch.domain.usecase.TestApiCallUseCase
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeHttpExecutor
import com.dd.sms.hook.testing.Fixtures
import com.dd.sms.hook.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiEditorViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeApiConfigRepository(listOf(Fixtures.config(id = 7L)))
    private val http = FakeHttpExecutor()

    private fun viewModel(id: Long): ApiEditorViewModel {
        val validator = ApiConfigValidator()
        val execution = CallExecution(RequestFactory(TemplateRenderer()), http, FakeCallLogRepository())

        return ApiEditorViewModel(
            SavedStateHandle(route = ApiEditorRoute(id)),
            GetApiConfigUseCase(repository),
            SaveApiConfigUseCase(repository, validator),
            TestApiCallUseCase(execution),
            validator,
        )
    }

    @Test
    fun `new editor starts with defaults and hides errors until save`() {
        val vm: ApiEditorViewModel = viewModel(ApiEditorRoute.NEW)

        assertTrue(vm.isNew)
        assertFalse(vm.state.value.loading)
        assertEquals(HttpMethod.POST, vm.state.value.draft.method)
        assertTrue(vm.state.value.errors.isEmpty())
        vm.onNameChange("")
        assertTrue(ApiConfigError.NAME_EMPTY in vm.state.value.errors)
        assertFalse(vm.state.value.visibleError(ApiConfigError.NAME_EMPTY))
    }

    @Test
    fun `existing config is loaded into the draft`() {
        val vm: ApiEditorViewModel = viewModel(7L)

        assertFalse(vm.isNew)
        assertEquals("Bank hook", vm.state.value.draft.name)
        assertEquals("2", vm.state.value.retriesText)
    }

    @Test
    fun `missing config reports not found`() = runTest {
        val vm: ApiEditorViewModel = viewModel(99L)

        vm.events.test {
            assertEquals(R.string.editor_not_found, (awaitItem() as ApiEditorEvent.Message).message.res)
        }
    }

    @Test
    fun `saving an invalid draft shows errors and a message, and stores nothing`() = runTest {
        val vm: ApiEditorViewModel = viewModel(ApiEditorRoute.NEW)

        vm.events.test {
            vm.onSave()
            assertEquals(R.string.editor_fix_errors, (awaitItem() as ApiEditorEvent.Message).message.res)
        }
        assertTrue(vm.state.value.visibleError(ApiConfigError.NAME_EMPTY))
        assertEquals(1, repository.configs.value.size)
    }

    @Test
    fun `editing fields then saving stores the config and emits Saved with its name`() = runTest {
        val vm: ApiEditorViewModel = viewModel(ApiEditorRoute.NEW)

        vm.onNameChange("Shop")
        vm.onUrlChange("https://shop.example/hook")
        vm.onMethodChange(HttpMethod.PUT)
        vm.onAddHeader()
        vm.onHeaderChange(0, vm.state.value.draft.headers[0].copy(name = "X-Key", value = "1"))
        vm.onSendersChange("VCB")
        vm.onTimeoutChange("30")
        vm.events.test {
            vm.onSave()
            assertEquals(ApiEditorEvent.Saved("Shop"), awaitItem())
        }
        val saved = repository.configs.value.last()
        assertEquals(HttpMethod.PUT, saved.method)
        assertEquals("X-Key", saved.headers.single().name)
        assertEquals("VCB", saved.filter.senders)
        assertEquals(30, saved.timeoutSeconds)
    }

    @Test
    fun `non numeric timeout is an error, removing a header clears its error`() {
        val vm: ApiEditorViewModel = viewModel(7L)

        vm.onTimeoutChange("abc")
        vm.onAddHeader()

        assertTrue(ApiConfigError.TIMEOUT_OUT_OF_RANGE in vm.state.value.errors)
        assertTrue(ApiConfigError.HEADER_NAME_EMPTY in vm.state.value.errors)
        vm.onRemoveHeader(0)
        assertFalse(ApiConfigError.HEADER_NAME_EMPTY in vm.state.value.errors)
    }

    @Test
    fun `test call ends in Done with the logged result`() {
        http.result = HttpResult.Response(500, "boom", 5)
        val vm: ApiEditorViewModel = viewModel(7L)

        vm.onRunTest("VCB", "hi")
        val done = vm.state.value.testCall as TestCallState.Done

        assertEquals(CallStatus.FAILED, done.log.status)
        assertEquals(500, done.log.responseCode)
        vm.onDismissTest()
        assertEquals(TestCallState.Idle, vm.state.value.testCall)
    }

    @Test
    fun `test call with an invalid url is blocked before any request`() {
        val vm: ApiEditorViewModel = viewModel(7L)

        vm.onUrlChange("not a url")
        vm.onRunTest("VCB", "hi")

        assertEquals(TestCallState.Idle, vm.state.value.testCall)
        assertTrue(vm.state.value.visibleError(ApiConfigError.URL_INVALID))
        assertTrue(http.requests.isEmpty())
    }
}
