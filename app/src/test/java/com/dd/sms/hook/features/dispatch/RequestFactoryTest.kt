package com.dd.sms.hook.features.dispatch

import com.dd.sms.hook.shared.domain.constants.HttpConstants
import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry
import com.dd.sms.hook.features.apiconfig.domain.model.HttpMethod
import com.dd.sms.hook.features.dispatch.domain.model.HttpRequestSpec
import com.dd.sms.hook.features.dispatch.domain.service.RequestFactory
import com.dd.sms.hook.features.dispatch.domain.service.TemplateRenderer
import com.dd.sms.hook.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RequestFactoryTest {
    private val factory = RequestFactory(TemplateRenderer())

    @Test
    fun `default json template is escaped and gets a json content type`() {
        val sms = Fixtures.sms(body = "Line \"1\"\nLine 2")
        val request: HttpRequestSpec = factory.build(Fixtures.config(), sms)

        assertEquals(
            "{\n  \"sender\": \"+84 901 234 567\",\n  \"message\": \"Line \\\"1\\\"\\nLine 2\",\n" +
                "  \"received_at\": \"2023-11-14T22:13:20Z\"\n}",
            request.body,
        )
        assertEquals(HeaderEntry(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.CONTENT_TYPE_JSON), request.headers.last())
    }

    @Test
    fun `explicit text content type disables json escaping and is not duplicated`() {
        val config = Fixtures.config().copy(
            bodyTemplate = "{{body}}",
            headers = listOf(HeaderEntry("content-type", "text/plain"), HeaderEntry("X-From", "{{sender}}")),
        )
        val request: HttpRequestSpec = factory.build(config, Fixtures.sms(body = "a\"b"))

        assertEquals("a\"b", request.body)
        assertEquals(listOf(HeaderEntry("content-type", "text/plain"), HeaderEntry("X-From", "+84 901 234 567")), request.headers)
    }

    @Test
    fun `GET sends no body and url-encodes placeholders`() {
        val config = Fixtures.config(url = " https://x.io/hook?from={{sender}}&sim={{sim}} ").copy(method = HttpMethod.GET)
        val request: HttpRequestSpec = factory.build(config, Fixtures.sms())

        assertNull(request.body)
        assertEquals("https://x.io/hook?from=%2B84+901+234+567&sim=1", request.url)
        assertEquals(emptyList<HeaderEntry>(), request.headers)
    }
}
