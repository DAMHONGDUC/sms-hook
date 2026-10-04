package com.dd.sms.hook.features.dispatch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivedSmsDao {
    @Insert
    suspend fun insert(entity: ReceivedSmsEntity): Long

    @Query("SELECT * FROM received_sms WHERE id = :id")
    suspend fun getById(id: Long): ReceivedSmsEntity?

    @Query("UPDATE received_sms SET matched_count = :count WHERE id = :id")
    suspend fun setMatchedCount(id: Long, count: Int)

    @Query("SELECT COUNT(*) FROM received_sms WHERE received_at >= :fromMillis")
    fun observeCountSince(fromMillis: Long): Flow<Int>

    @Query("DELETE FROM received_sms WHERE received_at < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long): Int

    @Query("DELETE FROM received_sms WHERE subscription_id = :subscriptionId")
    suspend fun deleteBySubscription(subscriptionId: Int): Int
}
