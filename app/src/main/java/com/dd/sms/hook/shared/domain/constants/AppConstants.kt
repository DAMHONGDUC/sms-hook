package com.dd.sms.hook.shared.domain.constants

/** Cross-feature constants: storage names, background work and notification identifiers. */
object DatabaseConstants {
    const val DB_NAME = "sms_forwarder.db"
    const val TABLE_API_CONFIGS = "api_configs"
    const val TABLE_CALL_LOGS = "call_logs"
    const val TABLE_RECEIVED_SMS = "received_sms"
}

object WorkConstants {
    const val TAG_API_CALL = "api_call"
    const val UNIQUE_LOG_CLEANUP = "log_cleanup"
    const val KEY_CONFIG_ID = "config_id"
    const val KEY_SMS_ID = "sms_id"
    const val KEY_TRIGGER = "trigger"
    /** Tag prefixes so the queue can be read back: WorkInfo exposes tags but not input data. */
    const val TAG_PREFIX_CONFIG = "config:"
    const val TAG_PREFIX_SMS = "sms:"
    const val TAG_PREFIX_TRIGGER = "trigger:"
    const val BACKOFF_SECONDS = 15L
    const val CLEANUP_INTERVAL_HOURS = 24L
}

object NotificationConstants {
    const val CHANNEL_SERVICE = "keep_alive"
    const val CHANNEL_FAILURES = "call_failures"
    const val CHANNEL_WORK = "api_work"
    const val ID_KEEP_ALIVE = 1001
    const val ID_WORK = 1002
    const val ID_FAILURE_BASE = 2000
}

object HttpConstants {
    const val MAX_STORED_BODY_BYTES = 16_000
    const val DEFAULT_TIMEOUT_SECONDS = 15
    const val MAX_TIMEOUT_SECONDS = 120
    const val DEFAULT_MAX_RETRIES = 3
    const val MAX_RETRIES_LIMIT = 10
    const val CONTENT_TYPE_HEADER = "Content-Type"
    const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"
    const val CONTENT_TYPE_TEXT = "text/plain; charset=utf-8"
}

object SmsConstants {
    const val EXTRA_SUBSCRIPTION_INDEX = "android.telephony.extra.SUBSCRIPTION_INDEX"
    const val EXTRA_SUBSCRIPTION_LEGACY = "subscription"
    const val NO_SUBSCRIPTION = -1
    const val RECEIVER_TIMEOUT_MILLIS = 9_000L
}
