package com.dd.sms.hook.features.dispatch.domain.service

import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode
import com.dd.sms.hook.features.apiconfig.domain.model.SmsFilter
import javax.inject.Inject

private const val TAG = "SmsMatcher"

/**
 * Decides whether an SMS triggers a config.
 * - CONTAINS: sender matches when it contains any listed entry (spaces, dashes, dots ignored); body contains keyword.
 * - REGEX: sender and keyword are regexes searched anywhere in the value; both ignore case.
 */
class SmsMatcher @Inject constructor() {
    private val separators: Regex = Regex("[,;\\n]")
    private val phoneNoise: Regex = Regex("[\\s\\-.()]")

    fun matches(filter: SmsFilter, sender: String, body: String): Boolean =
        senderMatches(filter, sender) && keywordMatches(filter, body)

    private fun senderMatches(filter: SmsFilter, sender: String): Boolean {
        if (filter.senders.isBlank()) return true

        return when (filter.mode) {
            MatchMode.REGEX -> regexFind(filter.senders, sender)
            MatchMode.CONTAINS -> {
                val normalizedSender: String = normalize(sender)
                filter.senders.split(separators)
                    .map { normalize(it) }
                    .filter { it.isNotEmpty() }
                    .any { normalizedSender.contains(it) }
            }
        }
    }

    private fun keywordMatches(filter: SmsFilter, body: String): Boolean {
        if (filter.keyword.isBlank()) return true

        return when (filter.mode) {
            MatchMode.REGEX -> regexFind(filter.keyword, body)
            MatchMode.CONTAINS -> body.contains(filter.keyword.trim(), ignoreCase = true)
        }
    }

    private fun normalize(value: String): String = value.replace(phoneNoise, "").lowercase()

    private fun regexFind(pattern: String, value: String): Boolean =
        try {
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(value)
        } catch (e: Exception) {
            AppLogger.e(TAG, "invalid regex treated as no match - {pattern: $pattern}", e)
            false
        }
}
