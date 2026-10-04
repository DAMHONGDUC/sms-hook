package com.dd.sms.hook.features.apiconfig.domain.model

import com.dd.sms.hook.shared.domain.constants.HttpConstants

/** Starting values for a new API config. */
object ApiConfigDefaults {
    const val BODY_TEMPLATE: String =
        "{\n  \"sender\": \"{{sender}}\",\n  \"message\": \"{{body}}\",\n  \"received_at\": \"{{received_at_iso}}\"\n}"

    fun newConfig(): ApiConfig = ApiConfig(
        id = ApiConfig.NEW_ID,
        name = "",
        url = "https://",
        method = HttpMethod.POST,
        headers = emptyList(),
        bodyTemplate = BODY_TEMPLATE,
        filter = SmsFilter.ANY,
        enabled = true,
        timeoutSeconds = HttpConstants.DEFAULT_TIMEOUT_SECONDS,
        maxRetries = HttpConstants.DEFAULT_MAX_RETRIES,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
