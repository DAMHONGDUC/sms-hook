package com.dd.sms.hook.features.calllog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dd.sms.hook.shared.domain.constants.DatabaseConstants

@Entity(
    tableName = DatabaseConstants.TABLE_CALL_LOGS,
    indices = [Index("created_at"), Index("config_id")],
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "config_id") val configId: Long?,
    @ColumnInfo(name = "config_name") val configName: String,
    @ColumnInfo(name = "sms_id") val smsId: Long?,
    @ColumnInfo(name = "sms_sender") val smsSender: String,
    @ColumnInfo(name = "sms_body") val smsBody: String,
    val url: String,
    val method: String,
    @ColumnInfo(name = "request_headers_json") val requestHeadersJson: String,
    @ColumnInfo(name = "request_body") val requestBody: String,
    @ColumnInfo(name = "response_code") val responseCode: Int?,
    @ColumnInfo(name = "response_body") val responseBody: String?,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    val attempt: Int,
    val status: String,
    val trigger: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

data class CallPointRow(
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val status: String,
)

data class CallSummaryRow(
    val total: Int,
    val success: Int,
    @ColumnInfo(name = "avg_duration") val avgDuration: Double?,
)

data class ApiBreakdownRow(
    @ColumnInfo(name = "config_id") val configId: Long?,
    @ColumnInfo(name = "config_name") val configName: String,
    val total: Int,
    val success: Int,
)
