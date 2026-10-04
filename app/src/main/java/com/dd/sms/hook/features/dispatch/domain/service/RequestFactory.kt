package com.dd.sms.hook.features.dispatch.domain.service

import com.dd.sms.hook.shared.domain.constants.HttpConstants
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry
import com.dd.sms.hook.features.dispatch.domain.model.HttpRequestSpec
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import javax.inject.Inject

/** Renders a config's templates against one SMS into a concrete request. */
class RequestFactory @Inject constructor(
    private val renderer: TemplateRenderer,
) {
    fun build(config: ApiConfig, sms: ReceivedSms): HttpRequestSpec {
        val values: Map<String, String> = variables(config, sms)
        val explicitContentType: String? = config.headers
            .firstOrNull { it.name.equals(HttpConstants.CONTENT_TYPE_HEADER, ignoreCase = true) }?.value
        val isJson: Boolean = explicitContentType?.contains("json", ignoreCase = true)
            ?: looksLikeJson(config.bodyTemplate)
        val body: String? = if (config.method.allowsBody && config.bodyTemplate.isNotBlank()) {
            renderer.render(config.bodyTemplate, values, if (isJson) Escaping.JSON else Escaping.NONE)
        } else {
            null
        }
        val headers: MutableList<HeaderEntry> = config.headers
            .map { it.copy(value = renderer.render(it.value, values, Escaping.NONE)) }
            .toMutableList()

        if (body != null && explicitContentType == null) {
            val contentType: String = if (isJson) HttpConstants.CONTENT_TYPE_JSON else HttpConstants.CONTENT_TYPE_TEXT
            headers += HeaderEntry(HttpConstants.CONTENT_TYPE_HEADER, contentType)
        }
        return HttpRequestSpec(
            url = renderer.render(config.url.trim(), values, Escaping.URL),
            method = config.method,
            headers = headers,
            body = body,
        )
    }

    private fun variables(config: ApiConfig, sms: ReceivedSms): Map<String, String> = mapOf(
        TemplateVariables.SENDER to sms.sender,
        TemplateVariables.BODY to sms.body,
        TemplateVariables.RECEIVED_AT to sms.receivedAt.toString(),
        TemplateVariables.RECEIVED_AT_ISO to TimeUtils.toIso(sms.receivedAt),
        TemplateVariables.SIM to sms.subscriptionId.toString(),
        TemplateVariables.CONFIG_NAME to config.name,
    )

    private fun looksLikeJson(template: String): Boolean {
        val trimmed: String = template.trimStart()

        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }
}
