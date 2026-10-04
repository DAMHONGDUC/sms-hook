package com.dd.sms.hook.features.dispatch.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.dd.sms.hook.shared.domain.constants.SmsConstants
import com.dd.sms.hook.shared.data.di.ApplicationScope
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.dispatch.domain.usecase.HandleIncomingSmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val TAG = "SmsReceiver"

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject
    lateinit var handleIncomingSms: HandleIncomingSmsUseCase

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages: Array<SmsMessage> = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: emptyArray()
        } catch (e: Exception) {
            AppLogger.e(TAG, "could not parse SMS intent - {extras: ${intent.extras?.keySet()}}", e)
            return
        }
        val subscriptionId: Int = intent.getIntExtra(
            SmsConstants.EXTRA_SUBSCRIPTION_INDEX,
            intent.getIntExtra(SmsConstants.EXTRA_SUBSCRIPTION_LEGACY, SmsConstants.NO_SUBSCRIPTION),
        )
        val receivedAt: Long = TimeUtils.now()
        // Multipart SMS arrive as several PDUs from the same sender; join them in order.
        val bySender: Map<String, String> = messages
            .groupBy { it.displayOriginatingAddress.orEmpty() }
            .mapValues { (_, parts) -> parts.joinToString(separator = "") { it.displayMessageBody.orEmpty() } }
        val pending: PendingResult = goAsync()

        AppLogger.i(TAG, "sms received - {parts: ${messages.size}, senders: ${bySender.keys}, sim: $subscriptionId}")
        appScope.launch {
            try {
                withTimeout(SmsConstants.RECEIVER_TIMEOUT_MILLIS) {
                    bySender.forEach { (sender, body) ->
                        handleIncomingSms(sender, body, receivedAt, subscriptionId)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "handling sms failed - {senders: ${bySender.keys}}", e)
            } finally {
                pending.finish()
            }
        }
    }
}
