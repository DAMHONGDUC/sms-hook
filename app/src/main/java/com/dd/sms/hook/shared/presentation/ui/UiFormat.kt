package com.dd.sms.hook.shared.presentation.ui

import java.util.Locale

/** Number formatting shared by screens. */
object UiFormat {
    private const val MILLIS_PER_SECOND = 1000.0
    private const val PERCENT = 100
    private const val LTR_ISOLATE = '⁦'
    private const val POP_ISOLATE = '⁩'

    /** Isolated left-to-right so "754 ms" does not flip to "ms 754" inside RTL text. */
    fun duration(ms: Long): String {
        val text: String =
            if (ms < MILLIS_PER_SECOND) "$ms ms" else String.format(Locale.ROOT, "%.1f s", ms / MILLIS_PER_SECOND)

        return "$LTR_ISOLATE$text$POP_ISOLATE"
    }

    fun percent(ratio: Float): String = "$LTR_ISOLATE${(ratio * PERCENT).toInt()}%$POP_ISOLATE"
}
