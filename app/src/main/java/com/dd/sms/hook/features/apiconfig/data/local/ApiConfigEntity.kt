package com.dd.sms.hook.features.apiconfig.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dd.sms.hook.shared.domain.constants.DatabaseConstants

@Entity(tableName = DatabaseConstants.TABLE_API_CONFIGS)
data class ApiConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val url: String,
    val method: String,
    @ColumnInfo(name = "headers_json") val headersJson: String,
    @ColumnInfo(name = "body_template") val bodyTemplate: String,
    @ColumnInfo(name = "sender_filter") val senderFilter: String,
    @ColumnInfo(name = "keyword_filter") val keywordFilter: String,
    @ColumnInfo(name = "match_mode") val matchMode: String,
    val enabled: Boolean,
    @ColumnInfo(name = "timeout_seconds") val timeoutSeconds: Int,
    @ColumnInfo(name = "max_retries") val maxRetries: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
