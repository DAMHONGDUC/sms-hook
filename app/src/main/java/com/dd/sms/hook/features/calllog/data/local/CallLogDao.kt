package com.dd.sms.hook.features.calllog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query(
        """
        SELECT * FROM call_logs
        WHERE (:status IS NULL OR status = :status)
          AND (:configId IS NULL OR config_id = :configId)
          AND (:query = '' OR config_name LIKE '%' || :query || '%' OR sms_sender LIKE '%' || :query || '%'
               OR sms_body LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%')
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observe(status: String?, configId: Long?, query: String, limit: Int): Flow<List<CallLogEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM call_logs WHERE status = :successStatus AND `trigger` != :excludedTrigger)")
    fun observeHasSuccess(successStatus: String, excludedTrigger: String): Flow<Boolean>

    /** Ids grow with every insert, so MAX(id) is the latest attempt of an SMS for this API. */
    @Query(
        """
        SELECT c.sms_id FROM call_logs c
        WHERE c.config_id = :configId AND c.sms_id IS NOT NULL AND c.`trigger` != :excludedTrigger
          AND c.status = :failedStatus
          AND c.id = (SELECT MAX(l.id) FROM call_logs l
                      WHERE l.config_id = c.config_id AND l.sms_id = c.sms_id AND l.`trigger` != :excludedTrigger)
        ORDER BY c.id ASC
        """
    )
    fun observeFailedSmsIds(configId: Long, failedStatus: String, excludedTrigger: String): Flow<List<Long>>

    @Query("SELECT * FROM call_logs WHERE sms_id = :smsId AND config_id = :configId ORDER BY created_at ASC")
    fun observeAttempts(smsId: Long, configId: Long): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE id = :id")
    fun observeById(id: Long): Flow<CallLogEntity?>

    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getById(id: Long): CallLogEntity?

    @Insert
    suspend fun insert(entity: CallLogEntity): Long

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()

    @Query("DELETE FROM call_logs WHERE created_at < :epochMillis")
    suspend fun deleteOlderThan(epochMillis: Long): Int

    @Query("DELETE FROM call_logs WHERE config_id = :configId")
    suspend fun deleteByConfig(configId: Long): Int

    @Query(
        """
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN status = :successStatus THEN 1 ELSE 0 END), 0) AS success,
               AVG(duration_ms) AS avg_duration
        FROM call_logs
        WHERE created_at >= :fromMillis AND `trigger` != :excludedTrigger
        """
    )
    fun observeSummary(fromMillis: Long, successStatus: String, excludedTrigger: String): Flow<CallSummaryRow>

    @Query(
        """
        SELECT created_at, status FROM call_logs
        WHERE created_at >= :fromMillis AND `trigger` != :excludedTrigger
        """
    )
    fun observeTimeline(fromMillis: Long, excludedTrigger: String): Flow<List<CallPointRow>>

    @Query(
        """
        SELECT config_id, MAX(config_name) AS config_name, COUNT(*) AS total,
               SUM(CASE WHEN status = :successStatus THEN 1 ELSE 0 END) AS success
        FROM call_logs
        WHERE created_at >= :fromMillis AND `trigger` != :excludedTrigger
        GROUP BY config_id
        ORDER BY total DESC
        LIMIT :limit
        """
    )
    fun observeBreakdown(
        fromMillis: Long,
        successStatus: String,
        excludedTrigger: String,
        limit: Int,
    ): Flow<List<ApiBreakdownRow>>
}
