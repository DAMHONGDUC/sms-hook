package com.dd.sms.hook.shared.domain.logging

import android.util.Log

/** Single logging entry point: every line carries a tag and the data the action worked with. */
object AppLogger {
    private const val PREFIX = "SmsHook/"

    fun d(tag: String, message: String) {
        Log.d(PREFIX + tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(PREFIX + tag, message)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        Log.w(PREFIX + tag, message, error)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        Log.e(PREFIX + tag, message, error)
    }
}
