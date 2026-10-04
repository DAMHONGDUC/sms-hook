package com.dd.sms.hook.features.dispatch.data.repository

import com.dd.sms.hook.features.dispatch.data.local.ReceivedSmsDao
import com.dd.sms.hook.features.dispatch.data.local.ReceivedSmsEntity
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import com.dd.sms.hook.features.dispatch.domain.repository.ReceivedSmsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReceivedSmsRepositoryImpl @Inject constructor(
    private val dao: ReceivedSmsDao,
) : ReceivedSmsRepository {
    override suspend fun insert(sms: ReceivedSms): Long = dao.insert(
        ReceivedSmsEntity(sms.id, sms.sender, sms.body, sms.receivedAt, sms.subscriptionId, sms.matchedCount)
    )

    override suspend fun getById(id: Long): ReceivedSms? = dao.getById(id)?.let {
        ReceivedSms(it.id, it.sender, it.body, it.receivedAt, it.subscriptionId, it.matchedCount)
    }

    override suspend fun setMatchedCount(id: Long, count: Int) {
        dao.setMatchedCount(id, count)
    }

    override fun observeCountSince(fromMillis: Long): Flow<Int> = dao.observeCountSince(fromMillis)

    override suspend fun deleteOlderThan(epochMillis: Long): Int = dao.deleteOlderThan(epochMillis)

    override suspend fun deleteBySubscription(subscriptionId: Int): Int = dao.deleteBySubscription(subscriptionId)
}
