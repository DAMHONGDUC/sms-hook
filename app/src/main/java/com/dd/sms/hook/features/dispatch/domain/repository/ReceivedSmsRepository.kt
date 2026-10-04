package com.dd.sms.hook.features.dispatch.domain.repository

import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import kotlinx.coroutines.flow.Flow

interface ReceivedSmsRepository {
    suspend fun insert(sms: ReceivedSms): Long

    suspend fun getById(id: Long): ReceivedSms?

    suspend fun setMatchedCount(id: Long, count: Int)

    fun observeCountSince(fromMillis: Long): Flow<Int>

    suspend fun deleteOlderThan(epochMillis: Long): Int

    suspend fun deleteBySubscription(subscriptionId: Int): Int
}
