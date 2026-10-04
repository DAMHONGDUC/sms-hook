package com.dd.sms.hook.features.apiconfig.domain.service

import com.dd.sms.hook.shared.domain.constants.HttpConstants
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode
import java.net.URI
import javax.inject.Inject

enum class ApiConfigError {
    NAME_EMPTY,
    URL_INVALID,
    SENDER_REGEX_INVALID,
    KEYWORD_REGEX_INVALID,
    HEADER_NAME_EMPTY,
    TIMEOUT_OUT_OF_RANGE,
    RETRIES_OUT_OF_RANGE,
}

class ApiConfigValidator @Inject constructor() {
    private val placeholder: Regex = Regex("\\{\\{\\s*\\w+\\s*\\}\\}")

    fun validate(config: ApiConfig): Set<ApiConfigError> {
        val errors: MutableSet<ApiConfigError> = mutableSetOf()

        if (config.name.isBlank()) errors += ApiConfigError.NAME_EMPTY
        if (!isValidUrl(config.url)) errors += ApiConfigError.URL_INVALID
        if (config.filter.mode == MatchMode.REGEX) {
            if (!isValidRegex(config.filter.senders)) errors += ApiConfigError.SENDER_REGEX_INVALID
            if (!isValidRegex(config.filter.keyword)) errors += ApiConfigError.KEYWORD_REGEX_INVALID
        }
        if (config.headers.any { it.name.isBlank() }) errors += ApiConfigError.HEADER_NAME_EMPTY
        if (config.timeoutSeconds !in 1..HttpConstants.MAX_TIMEOUT_SECONDS) {
            errors += ApiConfigError.TIMEOUT_OUT_OF_RANGE
        }
        if (config.maxRetries !in 0..HttpConstants.MAX_RETRIES_LIMIT) {
            errors += ApiConfigError.RETRIES_OUT_OF_RANGE
        }

        return errors
    }

    /** Placeholders are swapped for a dummy segment so templated URLs still parse. */
    private fun isValidUrl(url: String): Boolean {
        val candidate: String = url.trim().replace(placeholder, "x")

        return try {
            val uri: URI = URI(candidate)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        } catch (e: Exception) {
            // Invalid input is an expected outcome here; the editor shows the error to the user.
            false
        }
    }

    private fun isValidRegex(pattern: String): Boolean {
        if (pattern.isBlank()) return true

        return try {
            Regex(pattern)
            true
        } catch (e: Exception) {
            // Invalid input is an expected outcome here; the editor shows the error to the user.
            false
        }
    }
}
